package com.xxh.ringbones.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.mapper.RingtoneMapper
import com.xxh.ringbones.domain.model.Ringtone
import com.xxh.ringbones.domain.repository.RingtoneRepository
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingtoneRepositoryImpl @Inject constructor(
    private val ringtoneDao: RingtoneDao
) : RingtoneRepository {

    override fun searchByTitle(query: String): Flow<List<Ringtone>> =
        ringtoneDao.searchByTitle(query).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun searchByCategory(category: String): Flow<List<Ringtone>> =
        ringtoneDao.searchByCategory(category).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun getById(id: Long): Flow<Ringtone?> =
        ringtoneDao.getById(id).map { entity -> entity?.let { RingtoneMapper.toDomain(it) } }

    override fun getAllPaged(): PagingSource<Int, Ringtone> {
        val source = ringtoneDao.getAllPaged()
        return object : PagingSource<Int, Ringtone>() {
            override fun getRefreshKey(state: PagingState<Int, Ringtone>): Int? =
                state.anchorPosition

            override suspend fun load(
                params: PagingSource.LoadParams<Int>
            ): PagingSource.LoadResult<Int, Ringtone> {
                return when (val result = source.load(params)) {
                    is PagingSource.LoadResult.Page -> PagingSource.LoadResult.Page(
                        data = result.data.map { RingtoneMapper.toDomain(it) },
                        prevKey = result.prevKey,
                        nextKey = result.nextKey
                    )
                    is PagingSource.LoadResult.Error -> PagingSource.LoadResult.Error(result.throwable)
                    is PagingSource.LoadResult.Invalid -> PagingSource.LoadResult.Invalid()
                }
            }
        }
    }

    override suspend fun insertAll(ringtones: List<Ringtone>) {
        ringtoneDao.insertAll(ringtones.map { RingtoneMapper.toEntity(it) })
    }

    override suspend fun updatePlayCount(id: Long, count: Int) {
        ringtoneDao.updatePlayCount(id, count)
    }

    override suspend fun updateLastPlayed(id: Long, timestamp: Long) {
        ringtoneDao.updateLastPlayed(id, timestamp)
    }

    override suspend fun updateDownloadPath(id: Long, path: String) {
        ringtoneDao.updateDownloadPath(id, path)
    }

    override suspend fun toggleFavorite(id: Long) {
        ringtoneDao.toggleFavorite(id)
    }

    override fun getByIds(ids: List<Long>): Flow<List<Ringtone>> =
        if (ids.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            // Chunk to avoid exceeding SQLite max variable number (default 999)
            chunkedQuery(ids)
        }

    /**
     * Splits [ids] into batches to avoid hitting SQLITE_MAX_VARIABLE_NUMBER (default 999),
     * then queries each batch and concatenates results.
     */
    private fun chunkedQuery(ids: List<Long>): Flow<List<Ringtone>> = flow {
        val batches = ids.chunked(MAX_SQL_VARIABLES)
        val allEntities = mutableListOf<RingtoneEntity>()
        for (batch in batches) {
            val entities = ringtoneDao.getByIds(batch).first()
            allEntities.addAll(entities)
        }
        emit(allEntities.map { RingtoneMapper.toDomain(it) })
    }

    companion object {
        /** SQLite max host parameters default is 999; keep well under that. */
        private const val MAX_SQL_VARIABLES = 800
    }

    override fun getDistinctCategories(): Flow<List<String>> =
        ringtoneDao.getDistinctCategories()

    override fun getTopPlayed(limit: Int): Flow<List<Ringtone>> =
        ringtoneDao.getTopPlayed(limit).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }

    override fun getCategoryCounts(): Flow<Map<String, Int>> =
        ringtoneDao.getCategoryCounts().map { counts ->
            counts.associate { it.category to it.count }
        }

    override fun getByUrlDomain(domain: String): Flow<List<Ringtone>> =
        ringtoneDao.getByUrlDomain(domain).map { entities ->
            entities.map { RingtoneMapper.toDomain(it) }
        }
}
