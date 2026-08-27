package com.viralclip.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.ProjectRepository
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.services.VideoProcessingPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<Project> = emptyList(),
    val isProcessing: Boolean = false,
    val processingState: ProcessingState = ProcessingState.Idle,
    val lastProcessedProject: Project? = null,
    val errorMessage: String? = null,
    val showImportDialog: Boolean = false,
    val isLoadingProjects: Boolean = true,
    val totalVideosProcessed: Int = 0,
    val totalClipsCreated: Int = 0,
    val showDeleteConfirmation: Long? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val clipRepository: ClipRepository,
    private val pipeline: VideoProcessingPipeline,
    private val ffmpegProcessor: FFmpegProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
        observeProcessingState()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true) }
            projectRepository.getRecentProjects(50).collect { projects ->
                _uiState.update {
                    it.copy(
                        recentProjects = projects,
                        isLoadingProjects = false,
                        totalVideosProcessed = projects.size
                    )
                }
            }
        }
        viewModelScope.launch {
            clipRepository.getAllClips().collect { clips ->
                _uiState.update { it.copy(totalClipsCreated = clips.size) }
            }
        }
    }

    private fun observeProcessingState() {
        viewModelScope.launch {
            pipeline.state.collect { state ->
                _uiState.update {
                    it.copy(
                        processingState = state,
                        isProcessing = state !is ProcessingState.Idle && state !is ProcessingState.Complete
                    )
                }
            }
        }
    }

    fun importVideo(context: Context, videoUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val videoInfo = ffmpegProcessor.getVideoInfo(videoUri)
                val fileName = getFileName(context, videoUri)

                val project = Project(
                    name = fileName.substringBeforeLast("."),
                    sourceVideoUri = videoUri.toString(),
                    duration = videoInfo.durationMs
                )
                val projectId = projectRepository.insertProject(project)

                val result = pipeline.processVideo(videoUri, context)

                val clipsWithProjectId = result.generatedClips.map {
                    it.copy(projectId = projectId)
                }
                if (clipsWithProjectId.isNotEmpty()) {
                    clipRepository.insertClips(clipsWithProjectId)
                }

                val thumbnailFile = File(context.cacheDir, "thumb_${projectId}.jpg")
                ffmpegProcessor.generateThumbnail(videoUri, 1000L, thumbnailFile)

                val savedProject = project.copy(
                    id = projectId,
                    thumbnailPath = thumbnailFile.absolutePath,
                    clips = clipsWithProjectId
                )
                projectRepository.updateProject(savedProject)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastProcessedProject = savedProject,
                        totalVideosProcessed = it.totalVideosProcessed + 1,
                        totalClipsCreated = it.totalClipsCreated + clipsWithProjectId.size
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "Failed to process video"
                    )
                }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            try {
                projectRepository.deleteProject(projectId)
                _uiState.update {
                    it.copy(
                        recentProjects = it.recentProjects.filter { p -> p.id != projectId },
                        showDeleteConfirmation = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete project") }
            }
        }
    }

    fun renameProject(projectId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                projectRepository.renameProject(projectId, newName)
                _uiState.update {
                    it.copy(
                        recentProjects = it.recentProjects.map { p ->
                            if (p.id == projectId) p.copy(name = newName, updatedAt = System.currentTimeMillis()) else p
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to rename project") }
            }
        }
    }

    fun duplicateProject(projectId: Long) {
        viewModelScope.launch {
            try {
                val newId = projectRepository.duplicateProject(projectId)
                loadProjects()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to duplicate project") }
            }
        }
    }

    fun showDeleteConfirmation(projectId: Long) {
        _uiState.update { it.copy(showDeleteConfirmation = projectId) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissProcessing() {
        _uiState.update { it.copy(processingState = ProcessingState.Idle) }
    }

    fun showImportDialog() {
        _uiState.update { it.copy(showImportDialog = true) }
    }

    fun hideImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    companion object {
        fun getFileName(context: Context, uri: Uri): String {
            var name = "video"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
            }
            return name
        }
    }
}
