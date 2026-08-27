package com.viralclip.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.domain.model.BrandPreset
import com.viralclip.app.domain.repository.BrandPresetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrandUiState(
    val presets: List<BrandPreset> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val editingPreset: BrandPreset? = null
)

@HiltViewModel
class BrandViewModel @Inject constructor(
    private val brandPresetRepository: BrandPresetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrandUiState())
    val uiState: StateFlow<BrandUiState> = _uiState.asStateFlow()

    init {
        loadPresets()
    }

    private fun loadPresets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            brandPresetRepository.getAllBrandPresets().collect { presets ->
                _uiState.update {
                    it.copy(presets = presets, isLoading = false)
                }
            }
        }
    }

    fun createPreset(name: String, primaryColor: Long, secondaryColor: Long, accentColor: Long) {
        viewModelScope.launch {
            val preset = BrandPreset(
                name = name,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                accentColor = accentColor
            )
            brandPresetRepository.insertBrandPreset(preset)
        }
    }

    fun createPresetFull(preset: BrandPreset) {
        viewModelScope.launch {
            brandPresetRepository.insertBrandPreset(preset)
        }
    }

    fun updatePreset(preset: BrandPreset) {
        viewModelScope.launch {
            brandPresetRepository.updateBrandPreset(preset)
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch {
            try {
                brandPresetRepository.deleteBrandPreset(id)
                _uiState.update {
                    it.copy(presets = it.presets.filter { p -> p.id != id })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete preset") }
            }
        }
    }

    fun seedDefaultPresets() {
        viewModelScope.launch {
            brandPresetRepository.getAllBrandPresets().first().let { existing ->
                if (existing.isEmpty()) {
                    val defaults = listOf(
                        BrandPreset(
                            name = "My Brand",
                            primaryColor = 0xFF7C3AED,
                            secondaryColor = 0xFFEC4899,
                            accentColor = 0xFF3B82F6
                        ),
                        BrandPreset(
                            name = "Tech Channel",
                            primaryColor = 0xFF3B82F6,
                            secondaryColor = 0xFF06B6D4,
                            accentColor = 0xFF10B981
                        ),
                        BrandPreset(
                            name = "Gaming",
                            primaryColor = 0xFFEF4444,
                            secondaryColor = 0xFFF97316,
                            accentColor = 0xFFFBBF24,
                            watermarkEnabled = true,
                            watermarkText = "@gamerpro"
                        ),
                        BrandPreset(
                            name = "Minimalist",
                            primaryColor = 0xFF1F2937,
                            secondaryColor = 0xFF6B7280,
                            accentColor = 0xFFF3F4F6
                        )
                    )
                    defaults.forEach { brandPresetRepository.insertBrandPreset(it) }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
