package com.xxh.ringbones.core.player.visualizer

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Captures raw PCM audio data from the Android [Visualizer] API for use
 * with [FFTProcessor] to produce real-time spectrum visualization.
 *
 * Uses [onWaveFormDataCapture] to obtain time-domain PCM samples (not FFT
 * frequency magnitudes), since [FFTProcessor] runs its own FFT. Each byte
 * in the waveform array is an 8-bit unsigned PCM sample value, which is
 * converted to signed [Short] before emission.
 *
 * PCM samples are pushed into a conflated [Channel] from the capture
 * callback thread and exposed as [pcmFlow] for coroutine-friendly
 * collection — no mutable callback reassignment, so data is never dropped
 * between capture registration and flow collection.
 *
 * Falls back gracefully if the Visualizer API is unavailable on the device.
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

    /**
     * Hot flow emitting PCM chunks at ~30fps. Data flows through a conflated
     * channel so collection can start at any time without missing frames.
     */
    val pcmFlow: Flow<ShortArray>

    /** Underlying system Visualizer instance, or null if unavailable. */
    private var visualizer: Visualizer? = null

    /** Conflated channel — only the latest frame is kept if collector is slow. */
    private val pcmChannel = Channel<ShortArray>(Channel.CONFLATED)

    init {
        val v: Visualizer? = try {
            Visualizer(audioSessionId).apply {
                captureSize = fftSize
            }
        } catch (e: Exception) {
            null
        }

        if (v != null) {
            // Set up capture listener that pushes directly into the channel.
            // No mutable callback reassignment — data flows immediately.
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int,
                    ) {
                        if (waveform != null && waveform.size >= fftSize) {
                            val shorts = ShortArray(fftSize)
                            for (i in 0 until fftSize) {
                                val unsignedByte = waveform[i].toInt() and 0xFF
                                shorts[i] = ((unsignedByte - 128) * 256).toShort()
                            }
                            pcmChannel.trySend(shorts)
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int,
                    ) {
                        // Not used — we capture waveform PCM instead
                    }
                },
                (captureRateMs * 1000).toInt().coerceAtMost(Int.MAX_VALUE),
                /* waveform = */ true,
                /* fft = */ false,
            )
            v.enabled = true
            visualizer = v
            isAvailable = true
            pcmFlow = pcmChannel.receiveAsFlow()
        } else {
            isAvailable = false
            pcmFlow = emptyPcmFlow()
        }
    }

    /** Release the system Visualizer. Safe to call multiple times. */
    fun release() {
        visualizer?.enabled = false
        visualizer?.release()
        pcmChannel.close()
    }

    private companion object {
        const val DEFAULT_CAPTURE_RATE_MS = 33L
        const val DEFAULT_FFT_SIZE = 256
    }
}

/** Creates an empty flow for the fallback path. */
private fun emptyPcmFlow(): Flow<ShortArray> = kotlinx.coroutines.flow.emptyFlow()