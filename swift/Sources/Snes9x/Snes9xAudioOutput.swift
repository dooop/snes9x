// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

import AVFoundation

final class Snes9xAudioOutput {
    private let engine = AVAudioEngine()
    private let player = AVAudioPlayerNode()
    private let lock = NSLock()
    private let format = AVAudioFormat(
        commonFormat: .pcmFormatInt16,
        sampleRate: 32_040,
        channels: 2,
        interleaved: true
    )!
    private var availableBuffers: [AVAudioPCMBuffer] = []

    init() {
        availableBuffers = (0..<6).compactMap { _ in
            AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 2_048)
        }
        engine.attach(player)
        engine.connect(player, to: engine.mainMixerNode, format: format)
    }

    func start() {
        guard !engine.isRunning else { return }
        do {
            try engine.start()
            player.play()
        } catch {
            // Video and input remain usable when the host cannot open audio.
        }
    }

    func enqueue(samples: UnsafePointer<Int16>, frameCount: Int) {
        guard frameCount > 0 else { return }
        lock.lock()
        let buffer = availableBuffers.popLast()
        lock.unlock()
        guard let buffer, frameCount <= buffer.frameCapacity, let destination = buffer.int16ChannelData?[0] else {
            return
        }

        buffer.frameLength = AVAudioFrameCount(frameCount)
        destination.update(from: samples, count: frameCount * 2)
        player.scheduleBuffer(buffer) { [weak self, buffer] in
            self?.lock.lock()
            self?.availableBuffers.append(buffer)
            self?.lock.unlock()
        }
    }

    func pause() { player.pause() }
    func resume() { if engine.isRunning { player.play() } }

    func stop() {
        player.stop()
        engine.stop()
    }
}
