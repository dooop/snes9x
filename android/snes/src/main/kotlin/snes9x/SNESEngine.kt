package snes9x

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import snes9x.internal.NativeSNES
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class SNESEngine(
    context: Context,
    val configuration: SNESConfiguration,
) : Closeable {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "Snes9x") }
    private val _state = MutableStateFlow<SNESState>(SNESState.Idle)
    private val _frame = MutableStateFlow<Bitmap?>(null)
    private val nativeLock = Any()

    val state: StateFlow<SNESState> = _state.asStateFlow()
    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()

    @Volatile private var paused = false

    @Volatile private var stopped = false
    private var handle = 0L
    private var audioTrack: AudioTrack? = null
    private var ownsClaim = false

    fun start() {
        if (_state.value !is SNESState.Idle && _state.value !is SNESState.Stopped) return
        if (!engineClaimed.compareAndSet(false, true)) {
            _state.value = SNESState.Failed("In diesem Prozess läuft bereits eine SNES-Engine.")
            return
        }
        ownsClaim = true
        stopped = false
        _state.value = SNESState.Loading
        executor.execute(::prepareAndRun)
    }

    fun pause() {
        val currentState = _state.value
        val nextState = currentState.afterPauseRequest()
        if (nextState == currentState) return
        paused = true
        synchronized(nativeLock) {
            if (handle != 0L) NativeSNES.resetInputs(handle)
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
        button: SNESButton,
        pressed: Boolean,
        player: Int = 0,
    ) {
        synchronized(nativeLock) {
            if (handle != 0L && player in 0..1) NativeSNES.setButton(handle, player, button.mask, pressed)
        }
    }

    fun reset(hard: Boolean = false) {
        synchronized(nativeLock) { if (handle != 0L) NativeSNES.reset(handle, hard) }
    }

    fun saveState(file: File): Boolean =
        synchronized(nativeLock) { handle != 0L && NativeSNES.saveState(handle, file.path) }

    fun loadState(file: File): Boolean =
        synchronized(nativeLock) { handle != 0L && NativeSNES.loadState(handle, file.path) }

    fun addCheat(code: String): Boolean = synchronized(nativeLock) { handle != 0L && NativeSNES.addCheat(handle, code) }

    fun clearCheats() {
        synchronized(nativeLock) { if (handle != 0L) NativeSNES.clearCheats(handle) }
    }

    override fun close() {
        stopped = true
        executor.shutdownNow()
        synchronized(nativeLock) { releaseNative() }
        releaseClaim()
        _frame.value = null
        _state.value = SNESState.Stopped
    }

    private fun prepareAndRun() {
        try {
            val runtimeDirectory = File(appContext.filesDir, "SNES").apply { check(mkdirs() || isDirectory) }
            val stagedROM = File(appContext.cacheDir, "snes-current.sfc")
            val digest = copyROMAndDigest(stagedROM)
            val saveDirectory = File(runtimeDirectory, "Saves").apply { check(mkdirs() || isDirectory) }
            val save = File(saveDirectory, "$digest.srm")

            synchronized(nativeLock) {
                if (stopped) return
                handle = NativeSNES.create(runtimeDirectory.path)
                check(handle != 0L) { "Snes9x konnte nicht initialisiert werden oder wird bereits verwendet." }
                check(NativeSNES.loadROM(handle, stagedROM.path, save.path)) { NativeSNES.lastError(handle) }
                audioTrack = createAudioTrack().also(AudioTrack::play)
            }
            _state.value = SNESState.Running
            runLoop()
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Throwable) {
            if (!stopped) {
                stopped = true
                synchronized(nativeLock) { releaseNative() }
                releaseClaim()
                _state.value = SNESState.Failed(error.message ?: "Unbekannter Snes9x-Fehler")
            }
        }
    }

    private fun copyROMAndDigest(destination: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        appContext.contentResolver.openInputStream(configuration.romUri).use { input ->
            requireNotNull(input) { "Die ROM-Datei konnte nicht geöffnet werden." }
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
        val frameNanos = (NativeSNES.frameDuration(handle) * 1_000_000_000.0).toLong()
        var nextFrame = System.nanoTime()

        while (!stopped) {
            if (paused) {
                Thread.sleep(10)
                nextFrame = System.nanoTime()
                continue
            }
            var width = 0
            var height = 0
            val audioSamples =
                synchronized(nativeLock) {
                    if (stopped || handle == 0L) return
                    check(NativeSNES.runFrame(handle)) { NativeSNES.lastError(handle) }
                    width = NativeSNES.videoWidth(handle)
                    height = NativeSNES.videoHeight(handle)
                    check(width in 1..MAX_WIDTH && height in 1..MAX_HEIGHT)
                    check(NativeSNES.copyVideo(handle, pixels) == width * height)
                    NativeSNES.copyAudio(handle, samples)
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

            nextFrame += frameNanos
            val remaining = nextFrame - System.nanoTime()
            if (remaining > 0) {
                Thread.sleep(remaining / 1_000_000, (remaining % 1_000_000).toInt())
            } else {
                nextFrame = System.nanoTime()
            }
        }
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
            NativeSNES.resetInputs(handle)
            NativeSNES.destroy(handle)
            handle = 0
        }
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
