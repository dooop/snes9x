// Copyright (C) 2026 Dominic Opitz
// SPDX-License-Identifier: LicenseRef-Snes9x

package com.snes9x

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.core.graphics.createBitmap
import com.snes9x.internal.NativeSnes9x
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Snes9xEngine(
    context: Context,
    val configuration: Snes9xConfiguration,
) : Closeable {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Snes9x") }
    private val _state = MutableStateFlow<Snes9xState>(Snes9xState.Idle)
    private val _frame = MutableStateFlow<Bitmap?>(null)
    private val nativeLock = Any()

    val state: StateFlow<Snes9xState> = _state.asStateFlow()
    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()

    @Volatile private var paused = false

    @Volatile private var stopped = false
    private var handle = 0L
    private var audioTrack: AudioTrack? = null
    private var ownsClaim = false

    @Volatile private var autosaveFile: File? = null

    fun start() {
        if (_state.value !is Snes9xState.Idle && _state.value !is Snes9xState.Stopped) return
        if (!engineClaimed.compareAndSet(false, true)) {
            _state.value = Snes9xState.Failed("A Snes9x engine is already running in this process.")
            return
        }
        ownsClaim = true
        stopped = false
        _state.value = Snes9xState.Loading
        executor.execute(::prepareAndRun)
    }

    fun pause() {
        val currentState = _state.value
        val nextState = currentState.afterPauseRequest()
        if (nextState == currentState) return
        paused = true
        synchronized(nativeLock) {
            if (handle != 0L) NativeSnes9x.resetInputs(handle)
            audioTrack?.pause()
        }
        _state.value = nextState
    }

    fun resume() {
        val currentState = _state.value
        val nextState = currentState.afterResumeRequest()
        if (nextState == currentState) return
        paused = false
        synchronized(nativeLock) { audioTrack?.play() }
        _state.value = nextState
    }

    fun setButton(
        button: Snes9xButton,
        pressed: Boolean,
        player: Int = 0,
    ) {
        synchronized(nativeLock) {
            if (handle != 0L && player in 0..1) NativeSnes9x.setButton(handle, player, button.mask, pressed)
        }
    }

    fun reset(hard: Boolean = false) {
        synchronized(nativeLock) { if (handle != 0L) NativeSnes9x.reset(handle, hard) }
    }

    fun saveState(file: File): Boolean =
        synchronized(nativeLock) { handle != 0L && NativeSnes9x.saveState(handle, file.path) }

    fun loadState(file: File): Boolean =
        synchronized(nativeLock) { handle != 0L && NativeSnes9x.loadState(handle, file.path) }

    /** Writes the automatic save state immediately. */
    fun writeAutosave(): Boolean = synchronized(nativeLock) { writeAutosaveLocked() }

    /**
     * Removes the automatic save state so the next start begins from the battery save.
     *
     * Re-reads the ROM to recover the save identity when no session has been prepared yet, so call
     * this off the main thread.
     */
    fun deleteAutosave(): Boolean {
        val file = autosaveFile ?: runCatching { autosaveFileForROM() }.getOrNull() ?: return false
        return !file.exists() || file.delete()
    }

    fun addCheat(code: String): Boolean =
        synchronized(nativeLock) { handle != 0L && NativeSnes9x.addCheat(handle, code) }

    fun clearCheats() {
        synchronized(nativeLock) { if (handle != 0L) NativeSnes9x.clearCheats(handle) }
    }

    override fun close() {
        stopped = true
        executor.shutdownNow()
        synchronized(nativeLock) {
            writeAutosaveLocked()
            releaseNative()
        }
        releaseClaim()
        _frame.value = null
        _state.value = Snes9xState.Stopped
    }

    private fun prepareAndRun() {
        try {
            val runtimeDirectory = runtimeDirectory().apply { check(mkdirs() || isDirectory) }
            val stagedROM = File(appContext.cacheDir, "snes9x-current.sfc")
            val digest = copyROMAndDigest(stagedROM)
            val save = configuration.resolveSaveFile(runtimeDirectory, digest).ensureParentDirectory()
            autosaveFile = configuration.resolveAutosaveFile(runtimeDirectory, digest)?.ensureParentDirectory()

            synchronized(nativeLock) {
                if (stopped) return
                handle = NativeSnes9x.create(runtimeDirectory.path)
                check(handle != 0L) { "Snes9x could not be initialized or is already in use." }
                check(NativeSnes9x.loadROM(handle, stagedROM.path, save.path)) { NativeSnes9x.lastError(handle) }
                restoreAutosaveLocked()
                audioTrack = createAudioTrack().also(AudioTrack::play)
            }
            _state.value = Snes9xState.Running
            runLoop()
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Throwable) {
            if (!stopped) {
                stopped = true
                synchronized(nativeLock) { releaseNative() }
                releaseClaim()
                _state.value = Snes9xState.Failed(error.message ?: "Unknown Snes9x error")
            }
        }
    }

    private fun runtimeDirectory(): File = File(appContext.filesDir, "Snes9x")

    private fun File.ensureParentDirectory(): File {
        val parent = requireNotNull(parentFile) { "The save location has no parent directory." }
        check(parent.mkdirs() || parent.isDirectory) { "The save directory could not be created." }
        return this
    }

    private fun autosaveFileForROM(): File? {
        if (!configuration.autosaveEnabled) return null
        val digest = MessageDigest.getInstance("SHA-256")
        appContext.contentResolver.openInputStream(configuration.romUri).use { input ->
            requireNotNull(input) { "The ROM file could not be opened." }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return configuration.resolveAutosaveFile(
            runtimeDirectory(),
            digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
    }

    private fun copyROMAndDigest(destination: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        appContext.contentResolver.openInputStream(configuration.romUri).use { input ->
            requireNotNull(input) { "The ROM file could not be opened." }
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun runLoop() {
        val pixels = IntArray(MAX_WIDTH * MAX_HEIGHT)
        val samples = ShortArray(MAX_AUDIO_SAMPLES)
        var bitmaps: Array<Bitmap>? = null
        var bitmapIndex = 0
        val frameNanos = (NativeSnes9x.frameDuration(handle) * 1_000_000_000.0).toLong()
        val autosaveNanos = configuration.autosaveIntervalSeconds * 1_000_000_000L
        var nextFrame = System.nanoTime()
        var nextAutosave = nextFrame + autosaveNanos
        var autosavedWhilePaused = false

        while (!stopped) {
            if (paused) {
                if (!autosavedWhilePaused) {
                    synchronized(nativeLock) { writeAutosaveLocked() }
                    autosavedWhilePaused = true
                }
                Thread.sleep(10)
                nextFrame = System.nanoTime()
                continue
            }
            autosavedWhilePaused = false
            var width = 0
            var height = 0
            val audioSamples =
                synchronized(nativeLock) {
                    if (stopped || handle == 0L) return
                    check(NativeSnes9x.runFrame(handle)) { NativeSnes9x.lastError(handle) }
                    width = NativeSnes9x.videoWidth(handle)
                    height = NativeSnes9x.videoHeight(handle)
                    check(width in 1..MAX_WIDTH && height in 1..MAX_HEIGHT)
                    check(NativeSnes9x.copyVideo(handle, pixels) == width * height)
                    NativeSnes9x.copyAudio(handle, samples)
                }

            if (bitmaps == null || bitmaps[0].width != width || bitmaps[0].height != height) {
                bitmaps = Array(2) { createBitmap(width, height) }
            }
            val bitmap = bitmaps[bitmapIndex]
            bitmapIndex = (bitmapIndex + 1) % bitmaps.size
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            _frame.value = bitmap

            if (audioSamples > 0) {
                synchronized(nativeLock) { audioTrack?.write(samples, 0, audioSamples, AudioTrack.WRITE_BLOCKING) }
            }

            if (autosaveNanos > 0 && autosaveFile != null && System.nanoTime() >= nextAutosave) {
                synchronized(nativeLock) { writeAutosaveLocked() }
                nextAutosave = System.nanoTime() + autosaveNanos
            }

            nextFrame += frameNanos
            val remaining = nextFrame - System.nanoTime()
            if (remaining > 0) {
                Thread.sleep(remaining / 1_000_000, (remaining % 1_000_000).toInt())
            } else {
                nextFrame = System.nanoTime()
            }
        }
    }

    private fun writeAutosaveLocked(): Boolean {
        val file = autosaveFile ?: return false
        return handle != 0L && NativeSnes9x.saveState(handle, file.path)
    }

    private fun restoreAutosaveLocked() {
        val file = autosaveFile ?: return
        if (handle == 0L) return
        // A missing or unreadable autosave is not an error; the battery save already loaded.
        if (file.isFile) NativeSnes9x.loadState(handle, file.path)
    }

    private fun releaseNative() {
        try {
            audioTrack?.stop()
        } catch (_: IllegalStateException) {
            // The track may not have reached its initialized playback state.
        }
        audioTrack?.release()
        audioTrack = null
        if (handle != 0L) {
            NativeSnes9x.resetInputs(handle)
            NativeSnes9x.destroy(handle)
            handle = 0
        }
        autosaveFile = null
    }

    private fun releaseClaim() {
        if (ownsClaim) {
            ownsClaim = false
            engineClaimed.set(false)
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val minimum =
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
        return AudioTrack
            .Builder()
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            ).setAudioFormat(
                AudioFormat
                    .Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            ).setBufferSizeInBytes(maxOf(minimum, MAX_AUDIO_SAMPLES * Short.SIZE_BYTES))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private companion object {
        const val MAX_WIDTH = 512
        const val MAX_HEIGHT = 478
        const val SAMPLE_RATE = 32_040
        const val MAX_AUDIO_SAMPLES = 4_096
        val engineClaimed = AtomicBoolean(false)
    }
}
