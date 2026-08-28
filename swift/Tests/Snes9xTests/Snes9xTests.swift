// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import CSnes9xCore
import Foundation
import Testing

@testable import Snes9x

@Test func nativeEngineClaimIsReleasedExactlyOnce() {
    let first = snes9x_engine_create(FileManager.default.temporaryDirectory.path)
    #expect(first != nil)
    #expect(snes9x_engine_create(FileManager.default.temporaryDirectory.path) == nil)
    snes9x_engine_destroy(first)

    let replacement = snes9x_engine_create(FileManager.default.temporaryDirectory.path)
    #expect(replacement != nil)
    snes9x_engine_destroy(replacement)
}

@Test func configurationKeepsROMURL() {
    let url = URL(fileURLWithPath: "/tmp/game.sfc")
    #expect(Snes9xConfiguration(romURL: url).romURL == url)
}

@Test func autosaveIsEnabledByDefault() {
    let configuration = Snes9xConfiguration(romURL: URL(fileURLWithPath: "/tmp/game.sfc"))
    #expect(configuration.autosaveEnabled)
    #expect(configuration.autosaveDirectory == nil)
    #expect(configuration.autosaveInterval == 30)
    #expect(configuration.saveDirectory == nil)
}

@Test func unconfiguredSaveDirectoriesLiveBesideEachOtherInTheRuntimeDirectory() {
    let configuration = Snes9xConfiguration(romURL: URL(fileURLWithPath: "/tmp/game.sfc"))
    #expect(configuration.resolvedSaveDirectory == Snes9xConfiguration.defaultSaveDirectory)
    #expect(configuration.resolvedAutosaveDirectory == Snes9xConfiguration.defaultAutosaveDirectory)
    #expect(configuration.resolvedSaveDirectory.lastPathComponent == "Saves")
    #expect(configuration.resolvedAutosaveDirectory.lastPathComponent == "Autosaves")
    #expect(
        configuration.resolvedSaveDirectory.deletingLastPathComponent()
            == configuration.resolvedAutosaveDirectory.deletingLastPathComponent()
    )
}

@Test func configuredSaveDirectoriesReplaceTheDefaults() {
    let saves = URL(fileURLWithPath: "/tmp/snes9x-saves", isDirectory: true)
    let autosaves = URL(fileURLWithPath: "/tmp/snes9x-autosaves", isDirectory: true)
    let configuration = Snes9xConfiguration(
        romURL: URL(fileURLWithPath: "/tmp/game.sfc"),
        saveDirectory: saves,
        autosaveDirectory: autosaves
    )
    #expect(configuration.resolvedSaveDirectory == saves)
    #expect(configuration.resolvedAutosaveDirectory == autosaves)
}

@Test func autosaveCanBeDisabled() {
    let configuration = Snes9xConfiguration(
        romURL: URL(fileURLWithPath: "/tmp/game.sfc"), autosaveEnabled: false)
    #expect(!configuration.autosaveEnabled)
}

@Test func controllerButtonsMatchTheLibretroSNESContract() {
    #expect(Snes9xControllerButton.b.rawValue == 0x01)
    #expect(Snes9xControllerButton.y.rawValue == 0x02)
    #expect(Snes9xControllerButton.a.rawValue == 0x100)
    #expect(Snes9xControllerButton.x.rawValue == 0x200)
    #expect(Snes9xControllerButton.l.rawValue == 0x400)
    #expect(Snes9xControllerButton.r.rawValue == 0x800)
}

@Test func controllerConfigurationDefaultsToAdaptiveSystemTheme() {
    let configuration = Snes9xControllerConfiguration()
    #expect(configuration.theme == .system)
    #expect(configuration.presentationMode == .automatic)
    #expect(configuration.hapticsEnabled)
    #expect(configuration.overlayOpacity == 0.72)
}

@Test func controllerConfigurationOffersOriginalThemes() {
    #expect(Snes9xControllerConfiguration(theme: .snes9x).theme == .snes9x)
    #expect(Snes9xControllerConfiguration(theme: .superFamicom).theme == .superFamicom)
}

@Test func controllerConfigurationClampsOverlayOpacity() {
    #expect(Snes9xControllerConfiguration(overlayOpacity: -1).overlayOpacity == 0)
    #expect(Snes9xControllerConfiguration(overlayOpacity: 2).overlayOpacity == 1)
}

@Test func controllerConfigurationPreservesCustomControllerLabel() {
    #expect(
        Snes9xControllerConfiguration(controllerLabel: "My App").resolvedControllerLabel == "My App"
    )
    #expect(Snes9xControllerConfiguration(controllerLabel: "").resolvedControllerLabel.isEmpty)
}

@Test func externalControllerHidesOnScreenControls() {
    #expect(
        shouldShowOnScreenControls(
            requested: true, isTelevision: false, hasExternalController: false))
    #expect(
        !shouldShowOnScreenControls(
            requested: true, isTelevision: false, hasExternalController: true))
}

@Test func televisionNeverShowsControlsAndPromptsWithoutController() {
    #expect(
        !shouldShowOnScreenControls(
            requested: true, isTelevision: true, hasExternalController: false))
    #expect(shouldShowControllerConnectionPrompt(isTelevision: true, hasExternalController: false))
    #expect(!shouldShowControllerConnectionPrompt(isTelevision: true, hasExternalController: true))
}
