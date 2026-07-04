package com.xxh.ringbones.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xxh.ringbones.core.datastore.UserPreferences
import com.xxh.ringbones.core.util.Constants
import com.xxh.ringbones.data.local.dao.FavoriteDao
import com.xxh.ringbones.data.local.dao.PlayHistoryDao
import com.xxh.ringbones.data.local.dao.RingtoneDao
import com.xxh.ringbones.data.local.dao.SearchHistoryDao
import com.xxh.ringbones.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Timeout duration for OkHttp connections and reads, in seconds. */
private const val HTTP_TIMEOUT_SECONDS = 30L

/**
 * Migration from v1 → v2: adds the `isFavorite` column to the `ringtones` table.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE ringtones ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * Provides application-scoped singleton dependencies: OkHttp network client,
 * DataStore UserPreferences.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Creates a singleton OkHttpClient with 30-second connect and read timeouts.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * Creates a singleton UserPreferences backed by Jetpack DataStore.
     */
    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    /**
     * Creates a singleton Room AppDatabase with all ringtone-related tables.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRingtoneDao(db: AppDatabase): RingtoneDao = db.ringtoneDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun providePlayHistoryDao(db: AppDatabase): PlayHistoryDao = db.playHistoryDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()
}
