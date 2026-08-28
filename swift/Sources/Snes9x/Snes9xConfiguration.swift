// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import Foundation

/// Runtime options for one Snes9x session.
public struct Snes9xConfiguration: Sendable, Equatable {
    public var romURL: URL
    public var saveDirectory: URL?
    /// Writes and restores an automatic save state for the loaded ROM.
    public var autosaveEnabled: Bool
    /// Directory for automatic save states; `nil` uses ``defaultAutosaveDirectory``.
    public var autosaveDirectory: URL?
    /// Seconds between automatic save states while the engine is running.
    public var autosaveInterval: TimeInterval
    public var automaticallyStarts: Bool
    public var showsTouchControls: Bool

    public init(
        romURL: URL,
        saveDirectory: URL? = nil,
        autosaveEnabled: Bool = true,
        autosaveDirectory: URL? = nil,
        autosaveInterval: TimeInterval = 30,
        automaticallyStarts: Bool = true,
        showsTouchControls: Bool = true
    ) {
        self.romURL = romURL
        self.saveDirectory = saveDirectory
        self.autosaveEnabled = autosaveEnabled
        self.autosaveDirectory = autosaveDirectory
        self.autosaveInterval = autosaveInterval
        self.automaticallyStarts = automaticallyStarts
        self.showsTouchControls = showsTouchControls
    }

    /// Directory holding battery saves when ``saveDirectory`` is `nil`.
    public static var defaultSaveDirectory: URL { runtimeDirectory("Saves") }

    /// Directory holding automatic save states when ``autosaveDirectory`` is `nil`.
    public static var defaultAutosaveDirectory: URL { runtimeDirectory("Autosaves") }

    /// Battery-save directory this configuration resolves to.
    public var resolvedSaveDirectory: URL { saveDirectory ?? Self.defaultSaveDirectory }

    /// Automatic save-state directory this configuration resolves to.
    public var resolvedAutosaveDirectory: URL { autosaveDirectory ?? Self.defaultAutosaveDirectory }

    private static func runtimeDirectory(_ name: String) -> URL {
        let base =
            FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        return base.appendingPathComponent("Snes9x", isDirectory: true)
            .appendingPathComponent(name, isDirectory: true)
    }
}

public enum Snes9xState: Equatable, Sendable {
    case idle
    case loading
    case running
    case paused
    case stopped
    case failed(String)
}

public enum Snes9xControllerButton: Int, CaseIterable, Sendable {
    case b = 0x01
    case y = 0x02
    case select = 0x04
    case start = 0x08
    case up = 0x10
    case down = 0x20
    case left = 0x40
    case right = 0x80
    case a = 0x100
    case x = 0x200
    case l = 0x400
    case r = 0x800
}
