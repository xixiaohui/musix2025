package com.xxh.ringbones.core.player.visualizer

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Captures raw PCM audio data from the Android [Visualizer] API for use
 * with [FFTProcessor] to produce real-time spectrum visualization.
 *
 * Wraps the system [Visualizer] in a coroutine-friendly [Flow] that emits
 * [ShortArray] PCM chunks at the configured [captureRateMs]. Falls back
 * gracefully if the Visualizer API is unavailable on the device.
 *
 * @param audioSessionId The ExoPlayer audio session ID (zero = system output mixer)
 * @param captureRateMs Interval between PCM capture callbacks in milliseconds (~30fps)
 * @param fftSize Size of each captured PCM chunk (must match [FFTProcessor.fftSize])
 */
class VisualizerCapture(
    audioSessionId: Int,
    captureRateMs: Long = DEFAULT_CAPTURE_RATE_MS,
    fftSize: Int = DEFAULT_FFT_SIZE,
) {
    /** Whether the Visualizer was successfully created and is capturing. */
    val isAvailable: Boolean

    /** Hot flow emitting PCM chunks at ~30fps. Empty if [isAvailable] is false. */
    val pcmFlow: Flow<ShortArray>

    /** Underlying system Visualizer instance, or null if unavailable. */
    private var visualizer: Visualizer? = null

    init {
        // Attempt to create the system Visualizer; null on failure means we
        // produce an empty flow and [isAvailable] is false.
        val v: Visualizer? = try {
            Visualizer(audioSessionId).apply {
                captureSize = fftSize
            }
        } catch (e: Exception) {
            null
        }

        if (v != null) {
            // Configure capture listener — the callbackFlow block below
            // will swap onFftBytes to the emitting lambda when collected.
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) {
                        // Not used — we capture FFT data instead
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        onFftBytes(fft)
                    }
                },
                (captureRateMs.toInt() * 1000).coerceAtMost(Int.MAX_VALUE).toInt(),
                /* waveform = */ false,
                /* fft = */ true,
            )
            v.enabled = true
            visualizer = v
            isAvailable = true
            pcmFlow = callbackFlow {
                onFftBytes = { bytes ->
                    if (bytes != null && bytes.size >= fftSize * 2) {
                        val shorts = ShortArray(fftSize)
                        for (i in 0 until fftSize) {
                            val low = bytes[i * 2].toInt() and 0xFF
                            val high = bytes[i * 2 + 1].toInt() and 0xFF
                            shorts[i] = ((high shl 8) or low).toShort()
                        }
                        trySend(shorts)
                    }
                }
                awaitClose { /* channel closed when flow collection stops */ }
            }
        } else {
            isAvailable = false
            pcmFlow = emptyPcmFlow()
        }
    }

    /** Mutable callback set by init — assigned when real FFT data arrives. */
    private var onFftBytes: (ByteArray?) -> Unit = {}

    /** Release the system Visualizer. Safe to call multiple times. */
    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
    }

    private companion object {
        const val DEFAULT_CAPTURE_RATE_MS = 33L
        const val DEFAULT_FFT_SIZE = 256
    }
}

/** Creates an empty flow for the fallback path. */
private fun emptyPcmFlow(): Flow<ShortArray> = kotlinx.coroutines.flow.emptyFlow()
