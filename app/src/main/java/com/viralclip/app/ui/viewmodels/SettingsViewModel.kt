package com.viralclip.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.data.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = true,
    val themeMode: String = "System",
    val gpuAcceleration: Boolean = true,
    val autoSave: Boolean = true,
    val hapticFeedback: Boolean = true,
    val defaultPlatform: String = "TikTok",
    val defaultQuality: String = "High (1080p)",
    val defaultFps: Int = 30,
    val language: String = "en",
    val cacheSizeBytes: Long = 0L,
    val cacheSizeMb: Long = 0L,
    val totalProcessedVideos: Int = 0,
    val totalExportedClips: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.preferences.collect { prefs ->
                _uiState.update {
                    SettingsUiState(
                        darkMode = prefs.darkMode,
                        themeMode = prefs.themeMode,
                        gpuAcceleration = prefs.gpuAcceleration,
                        autoSave = prefs.autoSave,
                        hapticFeedback = prefs.hapticFeedback,
                        defaultPlatform = prefs.defaultPlatform.name,
                        defaultQuality = prefs.defaultQuality.name,
                        defaultFps = prefs.defaultFps,
                        language = prefs.language,
                        cacheSizeBytes = prefs.cacheSizeMb * 1024 * 1024,
                        cacheSizeMb = prefs.cacheSizeMb,
                        totalProcessedVideos = prefs.totalProcessedVideos,
                        totalExportedClips = prefs.totalExportedClips
                    )
                }
            }
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateDarkMode(enabled)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.updateThemeMode(mode)
        }
    }

    fun updateGpuAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateGpuAcceleration(enabled)
        }
    }

    fun updateAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateAutoSave(enabled)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateHapticFeedback(enabled)
        }
    }

    fun updateDefaultFps(fps: Int) {
        viewModelScope.launch {
            preferencesManager.updateDefaultFps(fps)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.updateLanguage(language)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            preferencesManager.clearCacheSize()
        }
    }

    fun refreshCacheSize(context: Context) {
        viewModelScope.launch {
            val cacheDir = context.cacheDir
            val size = calculateCacheSize(cacheDir)
            preferencesManager.updateCacheSize(size / (1024 * 1024))
        }
    }

    private fun calculateCacheSize(file: java.io.File): Long {
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                size += calculateCacheSize(child)
            }
        } else {
            size += file.length()
        }
        return size
    }
}
