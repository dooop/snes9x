// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import Snes9x
import SwiftUI
import UniformTypeIdentifiers

struct ContentView: View {
    @State private var romURL: URL?
    @State private var isImportingROM = false

    var body: some View {
        romImporter(content)
            .onOpenURL { url in
                romURL = url
            }
    }

    private var content: some View {
        ZStack {
            if let romURL {
                Snes9x(configuration: Snes9xConfiguration(romURL: romURL))
                    .id(romURL)
            } else {
                #if os(tvOS)
                    ContentUnavailableView(
                        "No game file selected",
                        systemImage: "gamecontroller",
                        description: Text("Open a game file using a URL.")
                    )
                #else
                    Button("Open game file") {
                        isImportingROM = true
                    }
                    .buttonStyle(.borderedProminent)
                    .accessibilityIdentifier("openROMButton")
                #endif
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func romImporter<Content: View>(_ content: Content) -> some View {
        #if os(tvOS)
            content
        #else
            content.fileImporter(
                isPresented: $isImportingROM,
                allowedContentTypes: [.data]
            ) { result in
                if case .success(let url) = result {
                    romURL = url
                }
            }
        #endif
    }
}

#Preview {
    ContentView()
}
