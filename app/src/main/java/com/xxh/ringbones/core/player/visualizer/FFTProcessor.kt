package com.xxh.ringbones.core.player.visualizer

import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computes FFT magnitude bins from raw PCM audio samples.
 *
 * Uses a radix-2 Cooley–Tukey FFT implementation operating on short[]
 * (16-bit PCM) data. The output is a list of normalized magnitude values
 * in the range 0f–1f, suitable for Canvas-based spectrum visualizers.
 *
 * The processor handles windowing (Hann), real-input FFT packing,
 * magnitude computation, and dB-to-linear normalization.
 *
 * @param fftSize Must be a power of 2 (default 256 for 128 output bins)
 */
class FFTProcessor(
    private val fftSize: Int = DEFAULT_FFT_SIZE,
) {

    /** Precomputed Hann window coefficients. */
    private val window: FloatArray = FloatArray(fftSize) { i ->
        (0.5f * (1.0f - cos(2.0f * Math.PI.toFloat() * i / (fftSize - 1)))).toFloat()
    }

    /** Temporary buffers reused across calls to avoid allocations. */
    private val realBuffer = FloatArray(fftSize)
    private val imagBuffer = FloatArray(fftSize)
    private val magnitudeBuffer = FloatArray(fftSize / 2)

    /** Output bin count. */
    val binCount: Int = fftSize / 2

    /**
     * Process a chunk of 16-bit PCM samples and return normalized FFT magnitudes.
     *
     * If [samples] is shorter than [fftSize], the remaining bins are zero-padded.
     * If longer, only the first [fftSize] samples are used.
     *
     * @param samples Raw 16-bit PCM data
     * @return List of [binCount] magnitudes (0f–1f representing relative amplitude)
     */
    fun process(samples: ShortArray): List<Float> {
        val usable = samples.size.coerceAtMost(fftSize)

        // Apply window and copy to real buffer
        for (i in 0 until usable) {
            realBuffer[i] = samples[i].toFloat() * window[i]
        }
        for (i in usable until fftSize) {
            realBuffer[i] = 0f
        }
        imagBuffer.fill(0f)

        // Radix-2 FFT (in-place on realBuffer and imagBuffer)
        fft(realBuffer, imagBuffer, fftSize)

        // Compute magnitude for first half (second half is conjugate mirror)
        for (i in 0 until binCount) {
            val re = realBuffer[i]
            val im = imagBuffer[i]
            magnitudeBuffer[i] = sqrt(re * re + im * im)
        }

        // Normalize to 0f–1f using dB-like scaling for visual appeal
        val maxMag = magnitudeBuffer.maxOrNull() ?: 1f
        if (maxMag <= 0f) return List(binCount) { 0f }

        return List(binCount) { index ->
            val normalized = magnitudeBuffer[index] / maxMag
            // Apply log scaling for better visual representation
            val dbScaled = 20f * log10(normalized.coerceAtLeast(1e-6f))
            // Map from dB range [-120, 0] to [0, 1]
            ((dbScaled + 120f) / 120f).coerceIn(0f, 1f)
        }
    }

    /**
     * In-place radix-2 Cooley–Tukey FFT.
     *
     * @param re Real components (modified in-place)
     * @param im Imaginary components (modified in-place)
     * @param n Transform size (must be power of 2)
     */
    private fun fft(re: FloatArray, im: FloatArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n) {
            if (i > j) {
                re.swap(j, i)
                im.swap(j, i)
            }
            var m = n shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Butterfly computation
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val angle = -2.0f * Math.PI.toFloat() / size
            val wRe = cos(angle)
            val wIm = sin(angle)

            for (k in 0 until n step size) {
                var wTempRe = 1.0f
                var wTempIm = 0.0f

                for (h in 0 until halfSize) {
                    val evenIdx = k + h
                    val oddIdx = evenIdx + halfSize

                    val tRe = wTempRe * re[oddIdx] - wTempIm * im[oddIdx]
                    val tIm = wTempRe * im[oddIdx] + wTempIm * re[oddIdx]

                    re[oddIdx] = re[evenIdx] - tRe
                    im[oddIdx] = im[evenIdx] - tIm
                    re[evenIdx] += tRe
                    im[evenIdx] += tIm

                    val nextWRe = wTempRe * wRe - wTempIm * wIm
                    wTempIm = wTempRe * wIm + wTempIm * wRe
                    wTempRe = nextWRe
                }
            }
            size *= 2
        }
    }

    private companion object {
        /** Default FFT size (power of 2, yields 128 output bins). */
        const val DEFAULT_FFT_SIZE = 256
    }
}

/** Swap two elements in-place in a FloatArray. */
private fun FloatArray.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}