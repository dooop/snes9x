// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import SwiftUI

public struct Snes9xView: View {
    @ObservedObject private var engine: Snes9xEngine
    private let showsControls: Bool
    private let controllerConfiguration: Snes9xControllerConfiguration

    public init(
        engine: Snes9xEngine,
        showsControls: Bool = true,
        controllerConfiguration: Snes9xControllerConfiguration = .init()
    ) {
        self.engine = engine
        self.showsControls = showsControls
        self.controllerConfiguration = controllerConfiguration
    }

    public var body: some View {
        ZStack {
            Color.black
            if let frame = engine.frame {
                Image(decorative: frame, scale: 1)
                    .resizable()
                    .interpolation(.none)
                    .aspectRatio(4.0 / 3.0, contentMode: .fit)
            } else {
                status
            }
            if shouldShowOnScreenControls(
                requested: showsControls,
                isTelevision: isTelevision,
                hasExternalController: engine.hasConnectedController
            ) {
                Snes9xControls(engine: engine, configuration: controllerConfiguration)
            }
            if shouldShowControllerConnectionPrompt(
                isTelevision: isTelevision,
                hasExternalController: engine.hasConnectedController
            ) {
                controllerConnectionPrompt
            }
        }
        .ignoresSafeArea()
    }

    private var isTelevision: Bool {
        #if os(tvOS)
            true
        #else
            false
        #endif
    }

    private var controllerConnectionPrompt: some View {
        VStack(spacing: 18) {
            Image(systemName: "gamecontroller")
                .font(.system(size: 52))
            Text("Connect a controller to play")
                .font(.title2.bold())
        }
        .foregroundStyle(.white)
        .padding(32)
        .background(.black.opacity(0.72), in: RoundedRectangle(cornerRadius: 24))
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private var status: some View {
        switch engine.state {
        case .loading:
            ProgressView().tint(.white)
        case .failed(let message):
            ContentUnavailableView(
                "ROM could not be started", systemImage: "exclamationmark.triangle", description: Text(message)
            )
            .foregroundStyle(.white)
        default:
            Text(controllerConfiguration.resolvedControllerLabel)
                .font(.largeTitle.bold())
                .foregroundStyle(.secondary)
        }
    }
}

func shouldShowOnScreenControls(
    requested: Bool,
    isTelevision: Bool,
    hasExternalController: Bool
) -> Bool {
    requested && !isTelevision && !hasExternalController
}

func shouldShowControllerConnectionPrompt(
    isTelevision: Bool,
    hasExternalController: Bool
) -> Bool {
    isTelevision && !hasExternalController
}
