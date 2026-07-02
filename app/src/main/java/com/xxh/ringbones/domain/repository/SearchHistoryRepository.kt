package com.xxh.ringbones.domain.repository

import com.xxh.ringbones.domain.model.SearchHistory
import kotlinx.coroutines.flow.Flow

/** Contract for search history persistence. */
interface SearchHistoryRepository {
    fun getRecentSearches(limit: Int): Flow<List<SearchHistory>>
    suspend fun insertSearch(query: String)
    suspend fun clearHistory()
}
