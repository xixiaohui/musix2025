package com.xxh.ringbones.data.repository

import com.xxh.ringbones.data.local.dao.SearchHistoryDao
import com.xxh.ringbones.data.local.entity.SearchHistoryEntity
import com.xxh.ringbones.domain.model.SearchHistory
import com.xxh.ringbones.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun getRecentSearches(limit: Int): Flow<List<SearchHistory>> =
        searchHistoryDao.getRecentSearches(limit).map { entities ->
            entities.map { entity ->
                SearchHistory(
                    id = entity.id,
                    query = entity.query,
                    searchedAt = entity.searchedAt
                )
            }
        }

    override suspend fun insertSearch(query: String) {
        searchHistoryDao.insert(SearchHistoryEntity(query = query))
    }

    override suspend fun clearHistory() {
        searchHistoryDao.clearAll()
    }
}
