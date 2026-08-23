// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import SwiftUI

/// High-level SwiftUI host for one Snes9x ROM.
public struct Snes9x: View {
    @StateObject private var engine: Snes9xEngine
    private let configuration: Snes9xConfiguration
    private let controllerConfiguration: Snes9xControllerConfiguration

    public init(
        configuration: Snes9xConfiguration,
        controllerConfiguration: Snes9xControllerConfiguration = .init()
    ) {
        self.configuration = configuration
        self.controllerConfiguration = controllerConfiguration
        _engine = StateObject(wrappedValue: Snes9xEngine(configuration: configuration))
    }

    public init(
        rom: URL,
        controllerConfiguration: Snes9xControllerConfiguration = .init()
    ) {
        self.init(
            configuration: Snes9xConfiguration(romURL: rom),
            controllerConfiguration: controllerConfiguration
        )
    }

    public var body: some View {
        Snes9xView(
            engine: engine,
            showsControls: configuration.showsTouchControls,
            controllerConfiguration: controllerConfiguration
        )
        .onAppear {
            if configuration.automaticallyStarts { engine.start() }
        }
        .onDisappear { engine.stop() }
    }
}
