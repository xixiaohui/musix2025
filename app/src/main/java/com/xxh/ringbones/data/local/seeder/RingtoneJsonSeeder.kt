package com.xxh.ringbones.data.local.seeder

import android.content.Context
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.local.entity.RingtoneEntity
import com.xxh.ringbones.domain.model.Ringtone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/** Directory under assets/ that contains the ringtone JSON files. */
private const val JSON_ASSETS_DIR = "jsonres"

/**
 * Parses ringtone JSON files from assets and seeds them into the Room database.
 *
 * Run once on first app launch. Guarded by a DataStore flag so subsequent
 * launches skip seeding.
 *
 * Each JSON file contains an array of ringtone objects with fields:
 * title, author, time, url, type.
 */
@Singleton
class RingtoneJsonSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringtoneDao: RingtoneDao
) {

    /** Shared JSON parser configured to ignore unknown keys. */
    private val jsonParser = Json { ignoreUnknownKeys = true }

    /**
     * Reads all JSON files from [JSON_ASSETS_DIR], parses them into
     * [RingtoneEntity] instances, and inserts them into the database.
     *
     * @return The total number of ringtones seeded.
     */
    suspend fun seedFromAssets(): Int = withContext(Dispatchers.IO) {
        val assetManager = context.assets
        val fileNames = assetManager.list(JSON_ASSETS_DIR) ?: emptyArray()

        val allEntities = mutableListOf<RingtoneEntity>()

        for (fileName in fileNames) {
            if (!fileName.endsWith(".json")) continue

            val filePath = "$JSON_ASSETS_DIR/$fileName"
            val jsonString = assetManager.open(filePath).use { stream ->
                InputStreamReader(stream).readText()
            }

            val models = jsonParser.decodeFromString<List<JsonRingtoneModel>>(jsonString)

            val entities = models.map { model ->
                RingtoneEntity(
                    title = model.title,
                    author = model.author,
                    duration = model.time,
                    url = model.url,
                    mimeType = inferMimeType(model.url),
                    category = model.type
                )
            }

            allEntities.addAll(entities)
        }

        if (allEntities.isNotEmpty()) {
            // Clear old data to prevent duplicate/dead URLs on re-seed
            ringtoneDao.deleteAll()
            ringtoneDao.insertAll(allEntities)
        }

        allEntities.size
    }

    /**
     * Infers MIME type from the URL file extension.
     * Defaults to "audio/mpeg" for mp3 files, which is the only format
     * present in the bundled JSON data.
     */
    private fun inferMimeType(url: String): String {
        return when {
            url.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            url.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            url.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            url.endsWith(".flac", ignoreCase = true) -> "audio/flac"
            url.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            else -> "audio/mpeg"
        }
    }
}