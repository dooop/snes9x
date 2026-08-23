// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import Foundation
import SwiftUI

/// The visual identity used by the on-screen controller.
public enum Snes9xControllerTheme: Sendable, Equatable {
    /// Uses a translucent, system-material surface with the current accent color.
    case system
    /// Uses the two-tone purple action-button palette of the original North American controller.
    case snes9x
    /// Uses the four-color action-button palette of the original international controller.
    case superFamicom
}

/// Controls how the on-screen controller uses the space offered by its container.
public enum Snes9xControllerPresentationMode: Sendable, Equatable {
    /// Shows a controller body when space permits and a transparent overlay in landscape or compact heights.
    case automatic
    /// Always presents the controls inside a controller-shaped surface.
    case gamepad
    /// Always places translucent controls over the game content.
    case overlay
}

/// Optional color replacements applied after the selected theme is resolved.
public struct Snes9xControllerColorOverrides {
    public var body: Color?
    public var directionalPad: Color?
    public var actionButtons: Color?
    public var utilityButtons: Color?
    public var labels: Color?
    public var bodyLabel: Color?

    public init(
        body: Color? = nil,
        directionalPad: Color? = nil,
        actionButtons: Color? = nil,
        utilityButtons: Color? = nil,
        labels: Color? = nil,
        bodyLabel: Color? = nil
    ) {
        self.body = body
        self.directionalPad = directionalPad
        self.actionButtons = actionButtons
        self.utilityButtons = utilityButtons
        self.labels = labels
        self.bodyLabel = bodyLabel
    }
}

/// Appearance and layout options for the on-screen controller.
public struct Snes9xControllerConfiguration {
    public var theme: Snes9xControllerTheme
    public var presentationMode: Snes9xControllerPresentationMode
    public var colors: Snes9xControllerColorOverrides
    /// Text shown on the controller body. `nil` uses the main app bundle name; an empty string hides it.
    public var controllerLabel: String?
    /// Enables tactile feedback when an on-screen control is pressed.
    public var hapticsEnabled: Bool
    /// Opacity applied to button surfaces in overlay mode.
    public var overlayOpacity: Double

    public init(
        theme: Snes9xControllerTheme = .system,
        presentationMode: Snes9xControllerPresentationMode = .automatic,
        colors: Snes9xControllerColorOverrides = .init(),
        controllerLabel: String? = nil,
        hapticsEnabled: Bool = true,
        overlayOpacity: Double = 0.72
    ) {
        self.theme = theme
        self.presentationMode = presentationMode
        self.colors = colors
        self.controllerLabel = controllerLabel
        self.hapticsEnabled = hapticsEnabled
        self.overlayOpacity = min(max(overlayOpacity, 0), 1)
    }

    var resolvedControllerLabel: String {
        if let controllerLabel { return controllerLabel }
        for key in ["CFBundleDisplayName", "CFBundleName"] {
            if let value = Bundle.main.object(forInfoDictionaryKey: key) as? String, !value.isEmpty {
                return value
            }
        }
        return ProcessInfo.processInfo.processName
    }
}
