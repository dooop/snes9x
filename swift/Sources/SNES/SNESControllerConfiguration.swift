import SwiftUI

/// The visual identity used by the on-screen controller.
public enum SNESControllerTheme: Sendable, Equatable {
    /// Uses a translucent, system-material surface with the current accent color.
    case system
    /// Uses the gray, black, and red palette of the original SNES controller.
    case snes
    /// Uses the light-gray, dark-gray, and multicolor palette of the Super Famicom controller.
    case superFamicom
}

/// Controls how the on-screen controller uses the space offered by its container.
public enum SNESControllerPresentationMode: Sendable, Equatable {
    /// Shows a controller body when space permits and a transparent overlay in landscape or compact heights.
    case automatic
    /// Always presents the controls inside a controller-shaped surface.
    case gamepad
    /// Always places translucent controls over the game content.
    case overlay
}

/// Optional color replacements applied after the selected theme is resolved.
public struct SNESControllerColorOverrides {
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
public struct SNESControllerConfiguration {
    public var theme: SNESControllerTheme
    public var presentationMode: SNESControllerPresentationMode
    public var colors: SNESControllerColorOverrides
    /// Opacity applied to button surfaces in overlay mode.
    public var overlayOpacity: Double

    public init(
        theme: SNESControllerTheme = .system,
        presentationMode: SNESControllerPresentationMode = .automatic,
        colors: SNESControllerColorOverrides = .init(),
        overlayOpacity: Double = 0.72
    ) {
        self.theme = theme
        self.presentationMode = presentationMode
        self.colors = colors
        self.overlayOpacity = min(max(overlayOpacity, 0), 1)
    }
}
