package com.viralclip.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.ProjectRepository
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.CaptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val project: Project? = null,
    val clips: List<Clip> = emptyList(),
    val selectedClipIndex: Int = 0,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val isProcessing: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val undoStack: List<EditorAction> = emptyList(),
    val redoStack: List<EditorAction> = emptyList(),
    val showCaptionEditor: Boolean = false,
    val showFilters: Boolean = false,
    val showSpeedControl: Boolean = false,
    val selectedTool: EditorTool = EditorTool.TRIM
) {
    val selectedClip: Clip? get() = clips.getOrNull(selectedClipIndex)
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}

enum class EditorTool { TRIM, CAPTIONS, TEXT, EFFECTS, AUDIO, SPEED, ADJUST }

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val clipRepository: ClipRepository,
    private val captionRepository: CaptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.getProjectById(projectId).collect { project ->
                _uiState.update { it.copy(project = project) }
            }
        }
        viewModelScope.launch {
            clipRepository.getClipsByProjectId(projectId).collect { clips ->
                _uiState.update { it.copy(clips = clips) }
            }
        }
    }

    fun selectClip(index: Int) {
        _uiState.update { it.copy(selectedClipIndex = index.coerceIn(0, it.clips.lastIndex)) }
    }

    fun selectTool(tool: EditorTool) {
        _uiState.update { it.copy(selectedTool = tool) }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun updatePosition(positionMs: Long) {
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    // ─── Trim ──────────────────────────────────────

    fun trimClip(clipId: Long, newStartMs: Long, newEndMs: Long) {
        viewModelScope.launch {
            val clip = _uiState.value.clips.find { it.id == clipId } ?: return@launch
            val action = EditorAction.TrimClip(
                clipId = clipId,
                oldStart = clip.startTimeMs,
                oldEnd = clip.endTimeMs,
                newStart = newStartMs,
                newEnd = newEndMs
            )
            clipRepository.updateClip(
                clip.copy(startTimeMs = newStartMs, endTimeMs = newEndMs)
            )
            pushAction(action)
        }
    }

    // ─── Split ──────────────────────────────────────

    fun splitClip(clipId: Long, splitPositionMs: Long) {
        viewModelScope.launch {
            val clip = _uiState.value.clips.find { it.id == clipId } ?: return@launch
            val firstClip = clip.copy(
                name = "${clip.name} (1)",
                endTimeMs = splitPositionMs,
                order = clip.order
            )
            val secondClip = clip.copy(
                id = 0,
                name = "${clip.name} (2)",
                startTimeMs = splitPositionMs,
                order = clip.order + 1
            )
            clipRepository.deleteClip(clipId)
            clipRepository.insertClip(firstClip)
            clipRepository.insertClip(secondClip)
            pushAction(EditorAction.SplitClip(clipId, splitPositionMs))
        }
    }

    // ─── Speed ──────────────────────────────────────

    fun changeSpeed(clipId: Long, speed: Float) {
        viewModelScope.launch {
            val clip = _uiState.value.clips.find { it.id == clipId } ?: return@launch
            val action = EditorAction.ChangeSpeed(clipId, clip.speed, speed)
            clipRepository.updateClip(clip.copy(speed = speed))
            pushAction(action)
        }
    }

    // ─── Delete ──────────────────────────────────────

    fun deleteClip(clipId: Long) {
        viewModelScope.launch {
            val clip = _uiState.value.clips.find { it.id == clipId } ?: return@launch
            val index = _uiState.value.clips.indexOf(clip)
            clipRepository.deleteClip(clipId)
            pushAction(EditorAction.DeleteClip(clip, index))
            _uiState.update {
                it.copy(selectedClipIndex = (index - 1).coerceAtLeast(0))
            }
        }
    }

    // ─── Caption Style ──────────────────────────────

    fun updateCaptionStyle(clipId: Long, style: CaptionStyle) {
        viewModelScope.launch {
            val clip = _uiState.value.clips.find { it.id == clipId } ?: return@launch
            val action = EditorAction.UpdateCaptionStyle(clipId, clip.captionStyle, style)
            clipRepository.updateClip(clip.copy(captionStyle = style))
            pushAction(action)
        }
    }

    // ─── Filters ──────────────────────────────────────

    fun updateFilters(clipId: Long, filters: ClipFilters) {
        viewModelScope.launch {
            val clip = _uiState.value.clips.find { it.id == clipId } ?: return@launch
            val action = EditorAction.UpdateFilter(clipId, clip.filters, filters)
            clipRepository.updateClip(clip.copy(filters = filters))
            pushAction(action)
        }
    }

    // ─── Undo/Redo ──────────────────────────────────

    private fun pushAction(action: EditorAction) {
        _uiState.update {
            it.copy(
                undoStack = it.undoStack + action,
                redoStack = emptyList(),
                hasUnsavedChanges = true
            )
        }
    }

    fun undo() {
        val state = _uiState.value
        val action = state.undoStack.lastOrNull() ?: return
        viewModelScope.launch {
            when (action) {
                is EditorAction.TrimClip -> {
                    clipRepository.updateClip(
                        state.selectedClip?.copy(
                            startTimeMs = action.oldStart,
                            endTimeMs = action.oldEnd
                        ) ?: return@launch
                    )
                }
                is EditorAction.ChangeSpeed -> {
                    clipRepository.updateClip(
                        state.selectedClip?.copy(speed = action.oldSpeed) ?: return@launch
                    )
                }
                is EditorAction.UpdateCaptionStyle -> {
                    clipRepository.updateClip(
                        state.selectedClip?.copy(captionStyle = action.oldStyle) ?: return@launch
                    )
                }
                is EditorAction.UpdateFilter -> {
                    clipRepository.updateClip(
                        state.selectedClip?.copy(filters = action.oldFilters) ?: return@launch
                    )
                }
                else -> {}
            }
            _uiState.update {
                it.copy(
                    undoStack = it.undoStack.dropLast(1),
                    redoStack = it.redoStack + action
                )
            }
        }
    }

    fun redo() {
        val state = _uiState.value
        val action = state.redoStack.lastOrNull() ?: return
        viewModelScope.launch {
            when (action) {
                is EditorAction.TrimClip -> {
                    clipRepository.updateClip(
                        state.selectedClip?.copy(
                            startTimeMs = action.newStart,
                            endTimeMs = action.newEnd
                        ) ?: return@launch
                    )
                }
                is EditorAction.ChangeSpeed -> {
                    clipRepository.updateClip(
                        state.selectedClip?.copy(speed = action.newSpeed) ?: return@launch
                    )
                }
                else -> {}
            }
            _uiState.update {
                it.copy(
                    redoStack = it.redoStack.dropLast(1),
                    undoStack = it.undoStack + action
                )
            }
        }
    }

    // ─── Reorder ──────────────────────────────────────

    fun reorderClips(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val clips = _uiState.value.clips.toMutableList()
            val item = clips.removeAt(fromIndex)
            clips.add(toIndex, item)
            val reorderedClips = clips.mapIndexed { i, clip -> clip.copy(order = i) }
            clipRepository.updateClips(reorderedClips)
            _uiState.update { it.copy(clips = reorderedClips) }
        }
    }
}
