// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import CSNESCore
import Combine
import CoreGraphics
import CryptoKit
import Foundation

/// Owns the process-wide native Snes9x session and its serialized frame loop.
public final class SNESEngine: ObservableObject {
    private static let claimLock = NSLock()
    private static var engineClaimed = false

    @Published public private(set) var state: SNESState = .idle
    @Published public private(set) var frame: CGImage?
    @Published public private(set) var hasConnectedController = false

    public let configuration: SNESConfiguration

    private let queue = DispatchQueue(label: "snes9x.swift.engine", qos: .userInteractive)
    private let queueKey = DispatchSpecificKey<Void>()
    private var core: OpaquePointer?
    private var timer: DispatchSourceTimer?
    private var audio: SNESAudioOutput?
    private var controller: SNESGameController?
    private var paused = false
    private var securityScopedROM = false
    private var ownsClaim = false

    public init(configuration: SNESConfiguration) {
        self.configuration = configuration
        queue.setSpecific(key: queueKey, value: ())
    }

    deinit {
        performSynchronously { stopOnQueue() }
    }

    public func start() {
        guard state == .idle || state == .stopped else { return }
        if controller == nil {
            controller = SNESGameController(engine: self) { [weak self] connected in
                self?.hasConnectedController = connected
            }
        }
        Self.claimLock.lock()
        guard !Self.engineClaimed else {
            Self.claimLock.unlock()
            state = .failed("In diesem Prozess läuft bereits eine SNES-Engine.")
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
            guard let core = snes_engine_create(systemDirectory) else {
                self.fail("Snes9x konnte nicht initialisiert werden oder wird bereits verwendet.")
                return
            }
            self.core = core

            do {
                let saveURL = try self.saveURL(for: configuration.romURL, directory: configuration.saveDirectory)
                try FileManager.default.createDirectory(
                    at: saveURL.deletingLastPathComponent(),
                    withIntermediateDirectories: true
                )
                guard snes_engine_load_rom(core, configuration.romURL.path, saveURL.path) else {
                    self.fail(String(cString: snes_engine_last_error(core)))
                    return
                }
            } catch {
                self.fail("Die ROM- oder Speicherdatei konnte nicht vorbereitet werden: \(error.localizedDescription)")
                return
            }

            self.audio = SNESAudioOutput()
            self.audio?.start()
            self.installTimer(core: core)
            DispatchQueue.main.async { [weak self] in self?.state = .running }
        }
    }

    public func pause() {
        guard state == .running else { return }
        queue.async { [weak self] in
            guard let self, self.core != nil else { return }
            self.paused = true
            snes_engine_reset_inputs(self.core)
            self.audio?.pause()
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
            snes_engine_reset(core, hard)
        }
    }

    public func setButton(_ button: SNESControllerButton, player: Int = 0, pressed: Bool) {
        queue.async { [weak self] in
            guard let core = self?.core, player >= 0 else { return }
            snes_engine_set_button(core, UInt32(player), SNESButton(rawValue: UInt32(button.rawValue)), pressed)
        }
    }

    @discardableResult
    public func saveState(to url: URL) -> Bool {
        performSynchronously { core.map { snes_engine_save_state($0, url.path) } ?? false }
    }

    @discardableResult
    public func loadState(from url: URL) -> Bool {
        performSynchronously { core.map { snes_engine_load_state($0, url.path) } ?? false }
    }

    @discardableResult
    public func addCheat(code: String) -> Bool {
        performSynchronously { core.map { snes_engine_add_cheat_code($0, code) } ?? false }
    }

    public func clearCheats() {
        queue.async { [weak self] in self?.core.map(snes_engine_clear_cheats) }
    }

    private func installTimer(core: OpaquePointer) {
        let timer = DispatchSource.makeTimerSource(queue: queue)
        timer.schedule(deadline: .now(), repeating: snes_engine_frame_duration(core), leeway: .milliseconds(1))
        timer.setEventHandler { [weak self] in self?.runFrame(core: core) }
        self.timer = timer
        timer.resume()
    }

    private func runFrame(core: OpaquePointer) {
        guard !paused, snes_engine_run_frame(core) else { return }
        if let samples = snes_engine_audio_buffer(core) {
            audio?.enqueue(samples: samples, frameCount: Int(snes_engine_audio_frame_count(core)))
        }

        guard let pixels = snes_engine_video_buffer(core) else { return }
        let width = Int(snes_engine_video_width(core))
        let height = Int(snes_engine_video_height(core))
        guard width > 0, height > 0 else { return }
        let data = Data(bytes: pixels, count: Int(snes_engine_video_pixel_count(core)) * MemoryLayout<UInt32>.size)
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

    private func saveURL(for romURL: URL, directory: URL?) throws -> URL {
        let baseDirectory =
            directory
            ?? FileManager.default.urls(
                for: .applicationSupportDirectory,
                in: .userDomainMask
            )[0].appendingPathComponent("SNES/Saves", isDirectory: true)
        return baseDirectory.appendingPathComponent(try stableGameIdentifier(for: romURL)).appendingPathExtension("srm")
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

    private func stopOnQueue(finalState: SNESState = .stopped) {
        timer?.cancel()
        timer = nil
        audio?.stop()
        audio = nil
        if let core {
            snes_engine_reset_inputs(core)
            snes_engine_destroy(core)
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
