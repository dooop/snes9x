// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import CSNESCore
import Foundation
import Testing

@testable import SNES

@Test func nativeEngineClaimIsReleasedExactlyOnce() {
    let first = snes_engine_create(FileManager.default.temporaryDirectory.path)
    #expect(first != nil)
    #expect(snes_engine_create(FileManager.default.temporaryDirectory.path) == nil)
    snes_engine_destroy(first)

    let replacement = snes_engine_create(FileManager.default.temporaryDirectory.path)
    #expect(replacement != nil)
    snes_engine_destroy(replacement)
}

@Test func configurationKeepsROMURL() {
    let url = URL(fileURLWithPath: "/tmp/game.sfc")
    #expect(SNESConfiguration(romURL: url).romURL == url)
}

@Test func controllerButtonsMatchTheLibretroSNESContract() {
    #expect(SNESControllerButton.b.rawValue == 0x01)
    #expect(SNESControllerButton.y.rawValue == 0x02)
    #expect(SNESControllerButton.a.rawValue == 0x100)
    #expect(SNESControllerButton.x.rawValue == 0x200)
    #expect(SNESControllerButton.l.rawValue == 0x400)
    #expect(SNESControllerButton.r.rawValue == 0x800)
}

@Test func controllerConfigurationDefaultsToAdaptiveSystemTheme() {
    let configuration = SNESControllerConfiguration()
    #expect(configuration.theme == .system)
    #expect(configuration.presentationMode == .automatic)
    #expect(configuration.hapticsEnabled)
    #expect(configuration.overlayOpacity == 0.72)
}

@Test func controllerConfigurationOffersOriginalThemes() {
    #expect(SNESControllerConfiguration(theme: .snes).theme == .snes)
    #expect(SNESControllerConfiguration(theme: .superFamicom).theme == .superFamicom)
}

@Test func controllerConfigurationClampsOverlayOpacity() {
    #expect(SNESControllerConfiguration(overlayOpacity: -1).overlayOpacity == 0)
    #expect(SNESControllerConfiguration(overlayOpacity: 2).overlayOpacity == 1)
}

@Test func controllerConfigurationPreservesCustomControllerLabel() {
    #expect(SNESControllerConfiguration(controllerLabel: "My App").resolvedControllerLabel == "My App")
    #expect(SNESControllerConfiguration(controllerLabel: "").resolvedControllerLabel.isEmpty)
}

@Test func externalControllerHidesOnScreenControls() {
    #expect(shouldShowOnScreenControls(requested: true, isTelevision: false, hasExternalController: false))
    #expect(!shouldShowOnScreenControls(requested: true, isTelevision: false, hasExternalController: true))
}

@Test func televisionNeverShowsControlsAndPromptsWithoutController() {
    #expect(!shouldShowOnScreenControls(requested: true, isTelevision: true, hasExternalController: false))
    #expect(shouldShowControllerConnectionPrompt(isTelevision: true, hasExternalController: false))
    #expect(!shouldShowControllerConnectionPrompt(isTelevision: true, hasExternalController: true))
}
