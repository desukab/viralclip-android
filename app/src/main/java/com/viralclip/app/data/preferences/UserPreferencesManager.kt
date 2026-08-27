package com.viralclip.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.viralclip.app.domain.model.ExportQuality
import com.viralclip.app.domain.model.PlatformPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "viralclip_settings")

data class AppPreferences(
    val darkMode: Boolean = true,
    val followSystemTheme: Boolean = false,
    val themeMode: String = "System",
    val gpuAcceleration: Boolean = true,
    val autoSave: Boolean = true,
    val hapticFeedback: Boolean = true,
    val defaultPlatform: PlatformPreset = PlatformPreset.TIKTOK,
    val defaultQuality: ExportQuality = ExportQuality.HIGH,
    val defaultFps: Int = 30,
    val language: String = "en",
    val exportPath: String = "",
    val cacheSizeMb: Long = 0L,
    val maxCacheMb: Long = 2048L,
    val totalProcessedVideos: Int = 0,
    val totalExportedClips: Int = 0,
    val onboardingCompleted: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val crashReportsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val lowStorageWarningShown: Boolean = false,
    val lastBackupAt: Long = 0L,
    val cloudSyncEnabled: Boolean = false,
    val preferredVideoCodec: String = "h264",
    val preferredAudioCodec: String = "aac",
    val hardwareDecodeOnly: Boolean = false
)

class UserPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_FOLLOW_SYSTEM_THEME = booleanPreferencesKey("follow_system_theme")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_GPU_ACCELERATION = booleanPreferencesKey("gpu_acceleration")
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save")
        private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        private val KEY_DEFAULT_PLATFORM = stringPreferencesKey("default_platform")
        private val KEY_DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        private val KEY_DEFAULT_FPS = intPreferencesKey("default_fps")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_EXPORT_PATH = stringPreferencesKey("export_path")
        private val KEY_CACHE_SIZE = longPreferencesKey("cache_size_mb")
        private val KEY_MAX_CACHE = longPreferencesKey("max_cache_mb")
        private val KEY_TOTAL_PROCESSED = intPreferencesKey("total_processed")
        private val KEY_TOTAL_EXPORTED = intPreferencesKey("total_exported")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val KEY_CRASH_REPORTS = booleanPreferencesKey("crash_reports")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val KEY_LOW_STORAGE_SHOWN = booleanPreferencesKey("low_storage_shown")
        private val KEY_LAST_BACKUP = longPreferencesKey("last_backup")
        private val KEY_CLOUD_SYNC = booleanPreferencesKey("cloud_sync")
        private val KEY_VIDEO_CODEC = stringPreferencesKey("video_codec")
        private val KEY_AUDIO_CODEC = stringPreferencesKey("audio_codec")
        private val KEY_HW_DECODE_ONLY = booleanPreferencesKey("hw_decode_only")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val theme = if (prefs[KEY_FOLLOW_SYSTEM_THEME] == true) "System" else (prefs[KEY_THEME_MODE] ?: if (prefs[KEY_DARK_MODE] == false) "Light" else "Dark")
        AppPreferences(
            darkMode = prefs[KEY_DARK_MODE] ?: true,
            followSystemTheme = prefs[KEY_FOLLOW_SYSTEM_THEME] ?: false,
            themeMode = theme,
            gpuAcceleration = prefs[KEY_GPU_ACCELERATION] ?: true,
            autoSave = prefs[KEY_AUTO_SAVE] ?: true,
            hapticFeedback = prefs[KEY_HAPTIC_FEEDBACK] ?: true,
            defaultPlatform = prefs[KEY_DEFAULT_PLATFORM]
                ?.let { runCatching { PlatformPreset.valueOf(it) }.getOrNull() }
                ?: PlatformPreset.TIKTOK,
            defaultQuality = prefs[KEY_DEFAULT_QUALITY]
                ?.let { runCatching { ExportQuality.valueOf(it) }.getOrNull() }
                ?: ExportQuality.HIGH,
            defaultFps = prefs[KEY_DEFAULT_FPS] ?: 30,
            language = prefs[KEY_LANGUAGE] ?: "en",
            exportPath = prefs[KEY_EXPORT_PATH] ?: "",
            cacheSizeMb = prefs[KEY_CACHE_SIZE] ?: 0L,
            maxCacheMb = prefs[KEY_MAX_CACHE] ?: 2048L,
            totalProcessedVideos = prefs[KEY_TOTAL_PROCESSED] ?: 0,
            totalExportedClips = prefs[KEY_TOTAL_EXPORTED] ?: 0,
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            analyticsEnabled = prefs[KEY_ANALYTICS_ENABLED] ?: true,
            crashReportsEnabled = prefs[KEY_CRASH_REPORTS] ?: true,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            lowStorageWarningShown = prefs[KEY_LOW_STORAGE_SHOWN] ?: false,
            lastBackupAt = prefs[KEY_LAST_BACKUP] ?: 0L,
            cloudSyncEnabled = prefs[KEY_CLOUD_SYNC] ?: false,
            preferredVideoCodec = prefs[KEY_VIDEO_CODEC] ?: "h264",
            preferredAudioCodec = prefs[KEY_AUDIO_CODEC] ?: "aac",
            hardwareDecodeOnly = prefs[KEY_HW_DECODE_ONLY] ?: false
        )
    }

    suspend fun current(): AppPreferences = preferences.first()

    suspend fun updateDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun updateFollowSystemTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FOLLOW_SYSTEM_THEME] = enabled }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit {
            it[KEY_THEME_MODE] = mode
            when (mode) {
                "Light" -> {
                    it[KEY_DARK_MODE] = false
                    it[KEY_FOLLOW_SYSTEM_THEME] = false
                }
                "Dark" -> {
                    it[KEY_DARK_MODE] = true
                    it[KEY_FOLLOW_SYSTEM_THEME] = false
                }
                else -> {
                    it[KEY_FOLLOW_SYSTEM_THEME] = true
                }
            }
        }
    }

    suspend fun updateCacheSize(sizeMb: Long) {
        context.dataStore.edit { it[KEY_CACHE_SIZE] = sizeMb.coerceAtLeast(0L) }
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

    suspend fun updateDefaultPlatform(platform: PlatformPreset) {
        context.dataStore.edit { it[KEY_DEFAULT_PLATFORM] = platform.name }
    }

    suspend fun updateDefaultQuality(quality: ExportQuality) {
        context.dataStore.edit { it[KEY_DEFAULT_QUALITY] = quality.name }
    }

    suspend fun updateDefaultFps(fps: Int) {
        val safe = fps.coerceIn(15, 120)
        context.dataStore.edit { it[KEY_DEFAULT_FPS] = safe }
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun updateExportPath(path: String) {
        context.dataStore.edit { it[KEY_EXPORT_PATH] = path }
    }

    suspend fun updateMaxCacheMb(sizeMb: Long) {
        val safe = sizeMb.coerceAtLeast(256L)
        context.dataStore.edit { it[KEY_MAX_CACHE] = safe }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ANALYTICS_ENABLED] = enabled }
    }

    suspend fun setCrashReportsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CRASH_REPORTS] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setLowStorageWarningShown(shown: Boolean) {
        context.dataStore.edit { it[KEY_LOW_STORAGE_SHOWN] = shown }
    }

    suspend fun setLastBackupAt(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_BACKUP] = timestamp }
    }

    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CLOUD_SYNC] = enabled }
    }

    suspend fun updateVideoCodec(codec: String) {
        context.dataStore.edit { it[KEY_VIDEO_CODEC] = codec }
    }

    suspend fun updateAudioCodec(codec: String) {
        context.dataStore.edit { it[KEY_AUDIO_CODEC] = codec }
    }

    suspend fun setHardwareDecodeOnly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HW_DECODE_ONLY] = enabled }
    }

    suspend fun incrementProcessedVideos() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TOTAL_PROCESSED] ?: 0
            prefs[KEY_TOTAL_PROCESSED] = current + 1
        }
    }

    suspend fun incrementExportedClips(by: Int = 1) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_TOTAL_EXPORTED] ?: 0
            prefs[KEY_TOTAL_EXPORTED] = current + by
        }
    }

    suspend fun setCacheSizeMb(sizeMb: Long) {
        context.dataStore.edit { it[KEY_CACHE_SIZE] = sizeMb.coerceAtLeast(0L) }
    }

    suspend fun clearCacheSize() {
        context.dataStore.edit { it[KEY_CACHE_SIZE] = 0L }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
