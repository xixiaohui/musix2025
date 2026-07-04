package com.xxh.ringbones.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Top-level DataStore extension property — used only inside this file. */
private val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

/**
 * Key constants for user preference entries stored in DataStore.
 */
private object PreferenceKeys {
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    /** Guard flag: true after JSON ringtone data has been seeded into Room. Bump suffix to force re-seed. */
    val JSON_SEEDED_V5 = booleanPreferencesKey("json_seeded_v5")
}

/**
 * Manages user-level preferences backed by Jetpack DataStore (Preferences).
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Observable: whether the user has selected dark theme. */
    val isDarkTheme: Flow<Boolean> = context.userPreferencesStore.data.map { preferences ->
        preferences[PreferenceKeys.DARK_THEME] ?: false
    }

    /** Observable: whether dynamic color (Material You) is enabled. */
    val isDynamicColor: Flow<Boolean> = context.userPreferencesStore.data.map { preferences ->
        preferences[PreferenceKeys.DYNAMIC_COLOR] ?: true
    }

    /** Persist dark theme preference. */
    suspend fun setDarkTheme(enabled: Boolean) {
        context.userPreferencesStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_THEME] = enabled
        }
    }

    /** Persist dynamic color preference. */
    suspend fun setDynamicColor(enabled: Boolean) {
        context.userPreferencesStore.edit { preferences ->
            preferences[PreferenceKeys.DYNAMIC_COLOR] = enabled
        }
    }

    /** Observable: whether JSON ringtone data has been seeded into Room. */
    val isJsonSeeded: Flow<Boolean> = context.userPreferencesStore.data.map { preferences ->
        preferences[PreferenceKeys.JSON_SEEDED_V5] ?: false
    }

    /** Mark JSON seeding as complete so it never runs again. */
    suspend fun setJsonSeeded(seeded: Boolean) {
        context.userPreferencesStore.edit { preferences ->
            preferences[PreferenceKeys.JSON_SEEDED_V5] = seeded
        }
    }
}
