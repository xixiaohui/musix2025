package com.xxh.ringbones.domain.model

/** Domain model for a search query history entry. */
data class SearchHistory(
    val id: Long,
    val query: String,
    val searchedAt: Long
)
