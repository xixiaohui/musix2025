package com.xxh.ringbones.core.player.model

/**
 * Equalizer preset profiles.
 *
 * Applied by [com.xxh.ringbones.core.player.PlayerEngine] through
 * Media3 [androidx.media3.exoplayer.audio.AudioProcessor] or
 * [androidx.media3.common.audio.AudioProcessor] chain.
 */
enum class EqPreset {
    /** No equalizer adjustment. */
    FLAT,
    /** Boost low frequencies. */
    BASS_BOOST,
    /** Boost high frequencies. */
    TREBLE_BOOST,
    /** Enhance vocal clarity. */
    VOCAL,
    /** Classical music profile. */
    CLASSICAL,
    /** Rock music profile. */
    ROCK,
    /** Pop music profile. */
    POP,
    /** Placeholder for user-defined bands. */
    CUSTOM,
}