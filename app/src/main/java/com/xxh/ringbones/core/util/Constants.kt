package com.xxh.ringbones.core.util

/**
 * Application-wide constants. No magic numbers in business code.
 */
object Constants {
    /** Default pagination page size for Room PagingSource and API calls. */
    const val PAGE_SIZE = 20

    /** Maximum number of recent search history entries to keep. */
    const val MAX_SEARCH_HISTORY = 20

    /** Maximum number of recent play history entries to show. */
    const val MAX_RECENT_PLAYS = 50

    /** Maximum number of "most played" entries to compute. */
    const val MAX_MOST_PLAYED = 20

    /** Database file name. */
    const val DATABASE_NAME = "ringtones_v2.db"
}
