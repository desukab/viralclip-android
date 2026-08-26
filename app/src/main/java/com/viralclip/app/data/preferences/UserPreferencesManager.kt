package com.viralclip.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "viralclip_settings")

data class AppPreferences(
    val darkMode: Boolean = true,
    val gpuAcceleration: Boolean = true,
    val autoSave: Boolean = true,
    val hapticFeedback: Boolean = true,
    val defaultPlatform: String = "TIKTOK",
    val defaultQuality: String = "HIGH",
    val defaultFps: Int = 30,
    val language: String = "en",
    val exportPath: String = "",
    val cacheSizeMb: Long = 0L,
    val totalProcessedVideos: Int = 0,
    val totalExportedClips: Int = 0
)

class UserPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_GPU_ACCELERATION = booleanPreferencesKey("gpu_acceleration")
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save")
        private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val KEY_DEFAULT_PLATFORM = stringPreferencesKey("default_platform")
        private val KEY_DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        private val KEY_DEFAULT_FPS = intPreferencesKey("default_fps")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_EXPORT_PATH = stringPreferencesKey("export_path")
        private val KEY_CACHE_SIZE = longPreferencesKey("cache_size_mb")
        private val KEY_TOTAL_PROCESSED = intPreferencesKey("total_processed")
        private val KEY_TOTAL_EXPORTED = intPreferencesKey("total_exported")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            darkMode = prefs[KEY_DARK_MODE] ?: true,
            gpuAcceleration = prefs[KEY_GPU_ACCELERATION] ?: true,
            autoSave = prefs[KEY_AUTO_SAVE] ?: true,
            hapticFeedback = prefs[KEY_HAPTIC_FEEDBACK] ?: true,
            defaultPlatform = prefs[KEY_DEFAULT_PLATFORM] ?: "TIKTOK",
            defaultQuality = prefs[KEY_DEFAULT_QUALITY] ?: "HIGH",
            defaultFps = prefs[KEY_DEFAULT_FPS] ?: 30,
            language = prefs[KEY_LANGUAGE] ?: "en",
            exportPath = prefs[KEY_EXPORT_PATH] ?: "",
            cacheSizeMb = prefs[KEY_CACHE_SIZE] ?: 0L,
            totalProcessedVideos = prefs[KEY_TOTAL_PROCESSED] ?: 0,
            totalExportedClips = prefs[KEY_TOTAL_EXPORTED] ?: 0
        )
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun updateGpuAcceleration(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GPU_ACCELERATION] = enabled }
    }

    suspend fun updateAutoSave(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_SAVE] = enabled }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun updateDefaultPlatform(platform: String) {
        context.dataStore.edit { it[KEY_DEFAULT_PLATFORM] = platform }
    }

    suspend fun updateDefaultQuality(quality: String) {
        context.dataStore.edit { it[KEY_DEFAULT_QUALITY] = quality }
    }

    suspend fun updateDefaultFps(fps: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_FPS] = fps }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun incrementProcessedVideos() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TOTAL_PROCESSED] ?: 0
            prefs[KEY_TOTAL_PROCESSED] = current + 1
        }
    }

    suspend fun incrementExportedClips() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TOTAL_EXPORTED] ?: 0
            prefs[KEY_TOTAL_EXPORTED] = current + 1
        }
    }

    suspend fun clearCacheSize() {
        context.dataStore.edit { it[KEY_CACHE_SIZE] = 0L }
    }
}
