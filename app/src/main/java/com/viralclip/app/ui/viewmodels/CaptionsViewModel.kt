package com.viralclip.app.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.CaptionRepository
import com.viralclip.app.core.ai.CaptionGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptionsUiState(
    val clip: Clip? = null,
    val captions: List<CaptionSegment> = emptyList(),
    val currentCaptionStyle: CaptionStyle = CaptionStyle(),
    val isGenerating: Boolean = false,
    val selectedPreset: CaptionPreset = CaptionPreset.DEFAULT,
    val selectedLanguage: String = "en",
    val previewText: String = "Your amazing captions appear here",
    val editingCaptionId: Long? = null,
    val editText: String = "",
    val availableLanguages: List<Pair<String, String>> = listOf(
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "pt" to "Portuguese",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "ar" to "Arabic",
        "hi" to "Hindi",
        "it" to "Italian",
        "ru" to "Russian"
    )
)

@HiltViewModel
class CaptionsViewModel @Inject constructor(
    private val clipRepository: ClipRepository,
    private val captionRepository: CaptionRepository,
    private val captionGenerator: CaptionGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptionsUiState())
    val uiState: StateFlow<CaptionsUiState> = _uiState.asStateFlow()

    fun loadClip(clipId: Long) {
        viewModelScope.launch {
            clipRepository.getClipById(clipId).collect { clip ->
                clip?.let {
                    _uiState.update { state ->
                        state.copy(
                            clip = it,
                            captions = it.captions,
                            currentCaptionStyle = it.captionStyle,
                            selectedPreset = it.captionStyle.preset
                        )
                    }
                }
            }
        }
    }

    fun generateCaptions() {
        val clip = _uiState.value.clip ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val result = captionGenerator.generateCaptions(
                    Uri.parse(clip.sourceVideoUri),
                    _uiState.value.selectedLanguage
                )
                // Filter captions to clip time range
                val clipCaptions = result.segments.map { seg ->
                    seg.copy(
                        clipId = clip.id,
                        startTimeMs = seg.startTimeMs - clip.startTimeMs,
                        endTimeMs = seg.endTimeMs - clip.startTimeMs
                    )
                }.filter { it.startTimeMs >= 0 }

                captionRepository.insertCaptions(clipCaptions)
                clipRepository.updateClip(clip.copy(captions = clipCaptions))
                _uiState.update { it.copy(captions = clipCaptions, isGenerating = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun updateCaptionPreset(preset: CaptionPreset) {
        val clip = _uiState.value.clip ?: return
        val newStyle = _uiState.value.currentCaptionStyle.copy(preset = preset)
        viewModelScope.launch {
            clipRepository.updateClip(clip.copy(captionStyle = newStyle))
            _uiState.update { it.copy(selectedPreset = preset, currentCaptionStyle = newStyle) }
        }
    }

    fun updateFontColor(color: Long) {
        updateStyle { it.copy(fontColor = color) }
    }

    fun updateHighlightColor(color: Long) {
        updateStyle { it.copy(highlightColor = color) }
    }

    fun updateFontSize(size: Int) {
        updateStyle { it.copy(fontSize = size) }
    }

    fun updatePosition(position: CaptionPosition) {
        updateStyle { it.copy(position = position) }
    }

    fun updateAnimation(animation: CaptionAnimation) {
        updateStyle { it.copy(animation = animation) }
    }

    fun updateOutlineWidth(width: Float) {
        updateStyle { it.copy(outlineWidth = width) }
    }

    fun updateOutlineColor(color: Long) {
        updateStyle { it.copy(outlineColor = color) }
    }

    fun updateCaseStyle(caseStyle: CaseStyle) {
        updateStyle { it.copy(caseStyle = caseStyle) }
    }

    fun updateLanguage(language: String) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun updateAlignment(alignment: Alignment) {
        updateStyle { it.copy(alignment = alignment) }
    }

    fun startEditingCaption(captionId: Long, text: String) {
        _uiState.update { it.copy(editingCaptionId = captionId, editText = text) }
    }

    fun updateEditText(text: String) {
        _uiState.update { it.copy(editText = text) }
    }

    fun saveCaptionEdit() {
        val state = _uiState.value
        val captionId = state.editingCaptionId ?: return
        val caption = state.captions.find { it.id == captionId } ?: return
        viewModelScope.launch {
            val updated = caption.copy(text = state.editText)
            captionRepository.updateCaption(updated)
            _uiState.update {
                it.copy(
                    captions = it.captions.map { c -> if (c.id == captionId) updated else c },
                    editingCaptionId = null,
                    editText = ""
                )
            }
        }
    }

    fun cancelCaptionEdit() {
        _uiState.update { it.copy(editingCaptionId = null, editText = "") }
    }

    fun deleteCaption(captionId: Long) {
        viewModelScope.launch {
            captionRepository.deleteCaptionsByClipId(captionId)
            _uiState.update { it.copy(captions = it.captions.filter { c -> c.id != captionId }) }
        }
    }

    private fun updateStyle(transform: (CaptionStyle) -> CaptionStyle) {
        val clip = _uiState.value.clip ?: return
        val newStyle = transform(_uiState.value.currentCaptionStyle)
        viewModelScope.launch {
            clipRepository.updateClip(clip.copy(captionStyle = newStyle))
            _uiState.update { it.copy(currentCaptionStyle = newStyle) }
        }
    }

}
