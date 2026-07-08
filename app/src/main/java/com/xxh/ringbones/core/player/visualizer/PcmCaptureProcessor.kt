package com.xxh.ringbones.core.player.visualizer

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference

/** Empty byte buffer constant matching AudioProcessor.EMPTY. */
private val EMPTY = ByteBuffer.allocateDirect(0)

/**
 * Taps into ExoPlayer's audio pipeline to capture raw 16-bit PCM data.
 *
 * Sits directly in the decoder → output path, requiring no extra
 * permissions. Each [queueInput] call publishes the latest PCM frame
 * via [latestFrame] for external FFT processing.
 *
 * Audio is passed through unchanged — this processor does not modify
 * or attenuate the signal.
 */
@UnstableApi
class PcmCaptureProcessor : AudioProcessor {

    /** Latest 256-sample mono PCM frame, updated from the audio thread. */
    val latestFrame = AtomicReference<ShortArray?>(null)

    private var channelCount: Int = 2
    private var outputBuffer: ByteBuffer = EMPTY
    private var inputEnded: Boolean = false

    override fun configure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        channelCount = inputFormat.channelCount.coerceAtLeast(1)
        return inputFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) {
            outputBuffer = EMPTY
            return
        }

        // Capture 256 mono samples from the first channel
        captureMono(inputBuffer)

        // Pass audio through unchanged
        outputBuffer = inputBuffer
    }

    private fun captureMono(buffer: ByteBuffer) {
        try {
            val frameSize = 256
            val samplesPerChannel = buffer.remaining() / (2 * channelCount) // 16-bit
            val step = (samplesPerChannel / frameSize).coerceAtLeast(1)
            val shorts = ShortArray(frameSize)

            for (i in 0 until frameSize) {
                val pos = buffer.position() + i * step * channelCount * 2
                if (pos + 1 >= buffer.limit()) break
                val lo = buffer.get(pos).toInt() and 0xFF
                val hi = buffer.get(pos + 1).toInt() and 0xFF
                shorts[i] = ((hi shl 8) or lo).toShort()
            }
            latestFrame.set(shorts)
        } catch (_: Exception) { }
    }

    override fun queueEndOfStream() {
        inputEnded = true
        outputBuffer = EMPTY
    }

    override fun getOutput(): ByteBuffer = outputBuffer

    override fun isEnded(): Boolean = inputEnded && outputBuffer == EMPTY

    override fun flush() {
        outputBuffer = EMPTY
        latestFrame.set(null)
    }

    override fun reset() {
        outputBuffer = EMPTY
        inputEnded = false
        latestFrame.set(null)
    }
}
