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
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<Project> = emptyList(),
    val isProcessing: Boolean = false,
    val processingState: ProcessingState = ProcessingState.Idle,
    val lastProcessedProject: Project? = null,
    val errorMessage: String? = null,
    val showImportDialog: Boolean = false
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
        viewModelScope.launch {
            projectRepository.getRecentProjects(10).collect { projects ->
                _uiState.update { it.copy(recentProjects = projects) }
            }
        }
        viewModelScope.launch {
            pipeline.state.collect { state ->
                _uiState.update { it.copy(processingState = state) }
            }
        }
    }

    fun importVideo(context: Context, videoUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                // Get video info
                val videoInfo = ffmpegProcessor.getVideoInfo(videoUri)
                val fileName = getFileName(context, videoUri)

                // Create project
                val project = Project(
                    name = fileName.substringBeforeLast("."),
                    sourceVideoUri = videoUri.toString(),
                    duration = videoInfo.durationMs
                )
                val projectId = projectRepository.insertProject(project)

                // Start processing
                val result = pipeline.processVideo(videoUri, context)

                // Save generated clips to database
                val clipsWithProjectId = result.generatedClips.map {
                    it.copy(projectId = projectId)
                }
                if (clipsWithProjectId.isNotEmpty()) {
                    clipRepository.insertClips(clipsWithProjectId)
                }

                // Generate thumbnail
                val thumbnailFile = java.io.File(context.cacheDir, "thumb_${projectId}.jpg")
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
                        lastProcessedProject = savedProject
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
            projectRepository.deleteProject(projectId)
        }
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
