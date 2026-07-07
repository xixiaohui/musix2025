package com.xxh.ringbones.core.player.visualizer

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Captures raw PCM audio data from the Android [Visualizer] API for use
 * with [FFTProcessor] to produce real-time spectrum visualization.
 *
 * Uses [onWaveFormDataCapture] to obtain time-domain PCM samples (not FFT
 * frequency magnitudes), since [FFTProcessor] runs its own FFT. Each byte
 * in the waveform array is an 8-bit unsigned PCM sample value, which is
 * converted to signed [Short] before emission.
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
            // Configure capture listener to receive waveform (PCM) data.
            // We capture raw PCM and let FFTProcessor run the FFT — using
            // onFftDataCapture would give us pre-computed frequency magnitudes
            // in a different format that cannot be fed through FFTProcessor.
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) {
                        onPcmBytes(waveform)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        // Not used — we capture waveform PCM instead
                    }
                },
                (captureRateMs.toInt() * 1000).coerceAtMost(Int.MAX_VALUE).toInt(),
                /* waveform = */ true,
                /* fft = */ false,
            )
            v.enabled = true
            visualizer = v
            isAvailable = true
            pcmFlow = callbackFlow {
                onPcmBytes = { bytes ->
                    if (bytes != null && bytes.size >= fftSize) {
                        val shorts = ShortArray(fftSize)
                        for (i in 0 until fftSize) {
                            // Convert unsigned byte to signed short: (byte - 128) * 256
                            val unsignedByte = bytes[i].toInt() and 0xFF
                            shorts[i] = ((unsignedByte - 128) * 256).toShort()
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

    /** Mutable callback set by init — assigned when real PCM waveform data arrives. */
    private var onPcmBytes: (ByteArray?) -> Unit = {}

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
