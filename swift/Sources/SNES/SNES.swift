import SwiftUI

/// High-level SwiftUI host for one Snes9x ROM.
public struct SNES: View {
    @StateObject private var engine: SNESEngine
    private let configuration: SNESConfiguration
    private let controllerConfiguration: SNESControllerConfiguration

    public init(
        configuration: SNESConfiguration,
        controllerConfiguration: SNESControllerConfiguration = .init()
    ) {
        self.configuration = configuration
        self.controllerConfiguration = controllerConfiguration
        _engine = StateObject(wrappedValue: SNESEngine(configuration: configuration))
    }

    public init(
        rom: URL,
        controllerConfiguration: SNESControllerConfiguration = .init()
    ) {
        self.init(
            configuration: SNESConfiguration(romURL: rom),
            controllerConfiguration: controllerConfiguration
        )
    }

    public var body: some View {
        SNESView(
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
