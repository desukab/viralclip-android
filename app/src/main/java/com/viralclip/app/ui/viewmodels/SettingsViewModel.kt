package com.viralclip.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.data.preferences.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = true,
    val gpuAcceleration: Boolean = true,
    val autoSave: Boolean = true,
    val hapticFeedback: Boolean = true,
    val defaultPlatform: String = "TikTok",
    val defaultQuality: String = "High (1080p)",
    val defaultFps: Int = 30,
    val language: String = "en",
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
                        gpuAcceleration = prefs.gpuAcceleration,
                        autoSave = prefs.autoSave,
                        hapticFeedback = prefs.hapticFeedback,
                        defaultPlatform = prefs.defaultPlatform,
                        defaultQuality = prefs.defaultQuality,
                        defaultFps = prefs.defaultFps,
                        language = prefs.language,
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

    fun clearCache() {
        viewModelScope.launch {
            preferencesManager.clearCacheSize()
        }
    }
}
