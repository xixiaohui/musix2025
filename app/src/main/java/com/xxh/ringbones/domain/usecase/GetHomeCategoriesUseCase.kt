package com.xxh.ringbones.domain.usecase

import javax.inject.Inject

/**
 * Provides the list of home screen categories.
 * Currently static; will be dynamic when online API is available (Phase 4).
 */
class GetHomeCategoriesUseCase @Inject constructor() {

    /** Category display name → database category value. */
    val categories: Map<Int, String> = mapOf(
        com.xxh.ringbones.R.string.hindi_bollywood to "Bollywood / Hindi",
        com.xxh.ringbones.R.string.tamil to "Tamil",
        com.xxh.ringbones.R.string.sms to "SMS  / Message Alert",
        com.xxh.ringbones.R.string.music to "Music",
        com.xxh.ringbones.R.string.malayalam to "Malayalam",
        com.xxh.ringbones.R.string.funny to "Funny",
        com.xxh.ringbones.R.string.sound to "Sound Effects",
        com.xxh.ringbones.R.string.miscellaneous to "Miscellaneous",
        com.xxh.ringbones.R.string.devotional to "Devotional",
        com.xxh.ringbones.R.string.baby to "Baby",
        com.xxh.ringbones.R.string.iphone to "Iphone"
    )

    operator fun invoke(): Map<Int, String> = categories
}
