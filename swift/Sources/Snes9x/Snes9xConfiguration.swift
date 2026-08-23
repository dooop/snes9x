// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import Foundation

/// Runtime options for one Snes9x session.
public struct Snes9xConfiguration: Sendable, Equatable {
    public var romURL: URL
    public var saveDirectory: URL?
    public var automaticallyStarts: Bool
    public var showsTouchControls: Bool

    public init(
        romURL: URL,
        saveDirectory: URL? = nil,
        automaticallyStarts: Bool = true,
        showsTouchControls: Bool = true
    ) {
        self.romURL = romURL
        self.saveDirectory = saveDirectory
        self.automaticallyStarts = automaticallyStarts
        self.showsTouchControls = showsTouchControls
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
