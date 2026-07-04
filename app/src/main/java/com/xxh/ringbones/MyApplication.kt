package com.xxh.ringbones

import android.app.Application
import android.util.Log
import com.xxh.ringbones.core.datastore.UserPreferences
import com.xxh.ringbones.data.local.seeder.RingtoneJsonSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point configured for Hilt dependency injection.
 * On first launch, seeds the Room database from bundled JSON ringtone files.
 */
@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var ringtoneJsonSeeder: RingtoneJsonSeeder

    @Inject
    lateinit var userPreferences: UserPreferences

    /** Application-scoped coroutine scope for background initialization. */
    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            seedJsonDataIfNeeded()
        }
    }

    /**
     * Seeds ringtone JSON data into Room on first launch.
     * Uses a DataStore flag to ensure seeding runs only once.
     */
    private suspend fun seedJsonDataIfNeeded() {
        try {
            val alreadySeeded = userPreferences.isJsonSeeded.first()
            if (alreadySeeded) return

            val count = ringtoneJsonSeeder.seedFromAssets()
            userPreferences.setJsonSeeded(true)
            Log.i(TAG, "JSON seeding complete: $count ringtones inserted")
        } catch (e: Exception) {
            Log.e(TAG, "JSON seeding failed", e)
        }
    }

    companion object {
        private const val TAG = "MyApplication"
    }
}