// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import CSnes9xCore
import Combine
import CoreGraphics
import CryptoKit
import Foundation

/// Owns the process-wide native Snes9x session and its serialized frame loop.
public final class Snes9xEngine: ObservableObject {
    private static let claimLock = NSLock()
    private static var engineClaimed = false

    @Published public private(set) var state: Snes9xState = .idle
    @Published public private(set) var frame: CGImage?
    @Published public private(set) var hasConnectedController = false

    public let configuration: Snes9xConfiguration

    private let queue = DispatchQueue(label: "snes9x.swift.engine", qos: .userInteractive)
    private let queueKey = DispatchSpecificKey<Void>()
    private var core: OpaquePointer?
    private var timer: DispatchSourceTimer?
    private var autosaveTimer: DispatchSourceTimer?
    private var autosaveURL: URL?
    private var audio: Snes9xAudioOutput?
    private var controller: Snes9xGameController?
    private var paused = false
    private var securityScopedROM = false
    private var ownsClaim = false

    public init(configuration: Snes9xConfiguration) {
        self.configuration = configuration
        queue.setSpecific(key: queueKey, value: ())
    }

    deinit {
        performSynchronously { stopOnQueue() }
    }

    public func start() {
        guard state == .idle || state == .stopped else { return }
        if controller == nil {
            controller = Snes9xGameController(engine: self) { [weak self] connected in
                self?.hasConnectedController = connected
            }
        }
        Self.claimLock.lock()
        guard !Self.engineClaimed else {
            Self.claimLock.unlock()
            state = .failed("A Snes9x engine is already running in this process.")
            return
        }
        Self.engineClaimed = true
        ownsClaim = true
        Self.claimLock.unlock()
        state = .loading
        let configuration = configuration

        queue.async { [weak self] in
            guard let self else { return }
            self.securityScopedROM = configuration.romURL.startAccessingSecurityScopedResource()
            let systemDirectory = configuration.romURL.deletingLastPathComponent().path
            guard let core = snes9x_engine_create(systemDirectory) else {
                self.fail("Snes9x could not be initialized or is already in use.")
                return
            }
            self.core = core

            do {
                let identifier = try self.stableGameIdentifier(for: configuration.romURL)
                let saveDirectory = configuration.resolvedSaveDirectory
                try FileManager.default.createDirectory(
                    at: saveDirectory, withIntermediateDirectories: true)
                let saveURL = saveDirectory.appendingPathComponent(identifier)
                    .appendingPathExtension("srm")
                if configuration.autosaveEnabled {
                    let autosaveDirectory = configuration.resolvedAutosaveDirectory
                    try FileManager.default.createDirectory(
                        at: autosaveDirectory, withIntermediateDirectories: true)
                    self.autosaveURL = autosaveDirectory.appendingPathComponent(identifier)
                        .appendingPathExtension("state")
                }
                guard snes9x_engine_load_rom(core, configuration.romURL.path, saveURL.path) else {
                    self.fail(String(cString: snes9x_engine_last_error(core)))
                    return
                }
            } catch {
                self.fail(
                    "The ROM or save file could not be prepared: \(error.localizedDescription)")
                return
            }

            self.restoreAutosaveOnQueue()
            self.audio = Snes9xAudioOutput()
            self.audio?.start()
            self.installTimer(core: core)
            self.installAutosaveTimer()
            DispatchQueue.main.async { [weak self] in self?.state = .running }
        }
    }

    public func pause() {
        guard state == .running else { return }
        queue.async { [weak self] in
            guard let self, self.core != nil else { return }
            self.paused = true
            snes9x_engine_reset_inputs(self.core)
            self.audio?.pause()
            self.writeAutosaveOnQueue()
            DispatchQueue.main.async { [weak self] in self?.state = .paused }
        }
    }

    public func resume() {
        guard state == .paused else { return }
        queue.async { [weak self] in
            guard let self, self.core != nil else { return }
            self.paused = false
            self.audio?.resume()
            DispatchQueue.main.async { [weak self] in self?.state = .running }
        }
    }

    public func stop() {
        queue.async { [weak self] in self?.stopOnQueue() }
    }

    public func reset(hard: Bool = false) {
        queue.async { [weak self] in
            guard let core = self?.core else { return }
            snes9x_engine_reset(core, hard)
        }
    }

    public func setButton(_ button: Snes9xControllerButton, player: Int = 0, pressed: Bool) {
        queue.async { [weak self] in
            guard let core = self?.core, player >= 0 else { return }
            snes9x_engine_set_button(
                core, UInt32(player), Snes9xButton(rawValue: UInt32(button.rawValue)), pressed)
        }
    }

    @discardableResult
    public func saveState(to url: URL) -> Bool {
        performSynchronously { core.map { snes9x_engine_save_state($0, url.path) } ?? false }
    }

    @discardableResult
    public func loadState(from url: URL) -> Bool {
        performSynchronously { core.map { snes9x_engine_load_state($0, url.path) } ?? false }
    }

    /// Writes the automatic save state immediately.
    @discardableResult
    public func writeAutosave() -> Bool {
        performSynchronously { writeAutosaveOnQueue() }
    }

    /// Removes the automatic save state so the next start begins from the battery save.
    @discardableResult
    public func deleteAutosave() -> Bool {
        // Resolve off the engine queue when no session is loaded so ROM hashing never stalls frames.
        guard let url = performSynchronously({ autosaveURL }) ?? autosaveURLForCurrentROM() else {
            return false
        }
        return (try? FileManager.default.removeItem(at: url)) != nil
    }

    @discardableResult
    public func addCheat(code: String) -> Bool {
        performSynchronously { core.map { snes9x_engine_add_cheat_code($0, code) } ?? false }
    }

    public func clearCheats() {
        queue.async { [weak self] in self?.core.map(snes9x_engine_clear_cheats) }
    }

    private func installTimer(core: OpaquePointer) {
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.schedule(
            deadline: .now(), repeating: snes9x_engine_frame_duration(core),
            leeway: .milliseconds(1))
        timer.setEventHandler { [weak self] in self?.runFrame(core: core) }
        self.timer = timer
        timer.resume()
    }

    private func runFrame(core: OpaquePointer) {
        guard !paused, snes9x_engine_run_frame(core) else { return }
        if let samples = snes9x_engine_audio_buffer(core) {
            audio?.enqueue(samples: samples, frameCount: Int(snes9x_engine_audio_frame_count(core)))
        }

        guard let pixels = snes9x_engine_video_buffer(core) else { return }
        let width = Int(snes9x_engine_video_width(core))
        let height = Int(snes9x_engine_video_height(core))
        guard width > 0, height > 0 else { return }
        let data = Data(
            bytes: pixels,
            count: Int(snes9x_engine_video_pixel_count(core)) * MemoryLayout<UInt32>.size)
        guard let provider = CGDataProvider(data: data as CFData),
            let image = CGImage(
                width: width,
                height: height,
                bitsPerComponent: 8,
                bitsPerPixel: 32,
                bytesPerRow: width * 4,
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.noneSkipLast.rawValue),
                provider: provider,
                decode: nil,
                shouldInterpolate: false,
                intent: .defaultIntent
            )
        else { return }
        DispatchQueue.main.async { [weak self] in self?.frame = image }
    }

    private func installAutosaveTimer() {
        guard configuration.autosaveEnabled, configuration.autosaveInterval > 0,
            autosaveURL != nil
        else { return }
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.schedule(
            deadline: .now() + configuration.autosaveInterval,
            repeating: configuration.autosaveInterval,
            leeway: .seconds(1))
        timer.setEventHandler { [weak self] in
            guard let self, !self.paused else { return }
            self.writeAutosaveOnQueue()
        }
        autosaveTimer = timer
        timer.resume()
    }

    @discardableResult
    private func writeAutosaveOnQueue() -> Bool {
        guard configuration.autosaveEnabled, let core, let url = autosaveURL,
            snes9x_engine_is_loaded(core)
        else { return false }
        return snes9x_engine_save_state(core, url.path)
    }

    private func restoreAutosaveOnQueue() {
        guard configuration.autosaveEnabled, let core, let url = autosaveURL,
            FileManager.default.fileExists(atPath: url.path)
        else { return }
        // A missing or unreadable autosave is not an error; the battery save already loaded.
        _ = snes9x_engine_load_state(core, url.path)
    }

    private func autosaveURLForCurrentROM() -> URL? {
        guard configuration.autosaveEnabled,
            let identifier = try? stableGameIdentifier(for: configuration.romURL)
        else { return nil }
        return configuration.resolvedAutosaveDirectory.appendingPathComponent(identifier)
            .appendingPathExtension("state")
    }

    private func stableGameIdentifier(for romURL: URL) throws -> String {
        let handle = try FileHandle(forReadingFrom: romURL)
        defer { try? handle.close() }
        var digest = SHA256()
        while let chunk = try handle.read(upToCount: 1024 * 1024), !chunk.isEmpty {
            digest.update(data: chunk)
        }
        return digest.finalize().map { String(format: "%02x", $0) }.joined()
    }

    private func fail(_ message: String) {
        stopOnQueue(finalState: .failed(message))
    }

    private func performSynchronously<T>(_ work: () -> T) -> T {
        if DispatchQueue.getSpecific(key: queueKey) != nil { return work() }
        return queue.sync(execute: work)
    }

    private func stopOnQueue(finalState: Snes9xState = .stopped) {
        timer?.cancel()
        timer = nil
        autosaveTimer?.cancel()
        autosaveTimer = nil
        writeAutosaveOnQueue()
        autosaveURL = nil
        audio?.stop()
        audio = nil
        if let core {
            snes9x_engine_reset_inputs(core)
            snes9x_engine_destroy(core)
            self.core = nil
        }
        if securityScopedROM {
            configuration.romURL.stopAccessingSecurityScopedResource()
            securityScopedROM = false
        }
        if ownsClaim {
            Self.claimLock.lock()
            Self.engineClaimed = false
            ownsClaim = false
            Self.claimLock.unlock()
        }
        DispatchQueue.main.async { [weak self] in
            self?.controller = nil
            self?.hasConnectedController = false
            self?.frame = nil
            self?.state = finalState
        }
    }
}
