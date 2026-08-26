package com.viralclip.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.ProjectRepository
import com.viralclip.app.util.Extensions.getExportDirectory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val clip: Clip? = null,
    val project: Project? = null,
    val selectedPlatform: PlatformPreset = PlatformPreset.TIKTOK,
    val selectedQuality: ExportQuality = ExportQuality.HIGH,
    val selectedFps: Int = 30,
    val selectedFormat: VideoFormat = VideoFormat.MP4,
    val includeCaptions: Boolean = true,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportComplete: Boolean = false,
    val exportPath: String? = null,
    val errorMessage: String? = null
) {
    val exportWidth: Int get() = selectedPlatform.width
    val exportHeight: Int get() = selectedPlatform.height
    val exportBitrate: Int get() = selectedQuality.bitrate
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val clipRepository: ClipRepository,
    private val projectRepository: ProjectRepository,
    private val ffmpegProcessor: FFmpegProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    /**
     * Load clip and its project using sequential collectors.
     * Fixed: No longer uses nested Flow collection which caused leaks.
     */
    fun loadClip(clipId: Long) {
        viewModelScope.launch {
            clipRepository.getClipById(clipId).first()?.let { clip ->
                _uiState.update { state -> state.copy(clip = clip) }
                // Load project sequentially after clip loads
                val project = projectRepository.getProjectById(clip.projectId).first()
                _uiState.update { state -> state.copy(project = project) }
            }
        }
    }

    fun selectPlatform(platform: PlatformPreset) {
        _uiState.update { it.copy(selectedPlatform = platform) }
    }

    fun selectQuality(quality: ExportQuality) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun selectFps(fps: Int) {
        _uiState.update { it.copy(selectedFps = fps) }
    }

    fun selectFormat(format: VideoFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun toggleCaptions() {
        _uiState.update { it.copy(includeCaptions = !it.includeCaptions) }
    }

    fun exportVideo(context: Context) {
        val state = _uiState.value
        val clip = state.clip ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f, errorMessage = null) }
            try {
                val outputDir = context.getExportDirectory()
                val originalName = state.project?.name?.take(50)?.replace(Regex("[^a-zA-Z0-9_\\- ]"), "") ?: "ViralClip"
                val outputFile = File(
                    outputDir,
                    "${originalName}_${state.selectedPlatform.displayName.replace(" ", "")}.${state.selectedFormat.extension}"
                )

                val sourceUri = Uri.parse(clip.sourceVideoUri)

                // First trim to clip boundaries, then export with target dimensions
                val trimmedFile = File(context.cacheDir, "trim_${clip.id}.mp4")
                val trimSuccess = ffmpegProcessor.trimVideo(
                    inputUri = sourceUri,
                    startTimeMs = clip.startTimeMs,
                    endTimeMs = clip.endTimeMs,
                    outputFile = trimmedFile,
                    onProgress = { progress ->
                        _uiState.update { it.copy(exportProgress = progress * 0.3f) }
                    }
                )

                if (!trimSuccess) {
                    throw Exception("Failed to trim video")
                }

                // Apply speed change if not 1x
                val speedFile = if (clip.speed != 1.0f) {
                    File(context.cacheDir, "speed_${clip.id}.mp4").also { file ->
                        val success = ffmpegProcessor.changeSpeed(
                            inputUri = Uri.fromFile(trimmedFile),
                            speed = clip.speed,
                            outputFile = file,
                            onProgress = { progress ->
                                _uiState.update { it.copy(exportProgress = 0.3f + progress * 0.3f) }
                            }
                        )
                        if (!success) throw Exception("Failed to apply speed change")
                    }
                } else null

                val preExportFile = speedFile ?: trimmedFile

                // Final export with target resolution
                val success = ffmpegProcessor.exportVideo(
                    inputUri = Uri.fromFile(preExportFile),
                    outputPath = outputFile.absolutePath,
                    width = state.exportWidth,
                    height = state.exportHeight,
                    bitrate = state.exportBitrate,
                    fps = state.selectedFps,
                    onProgress = { progress ->
                        _uiState.update { it.copy(exportProgress = 0.6f + progress * 0.4f) }
                    }
                )

                // Cleanup temp files
                trimmedFile.delete()
                speedFile?.delete()

                if (success) {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportProgress = 1f,
                            exportComplete = true,
                            exportPath = outputFile.absolutePath
                        )
                    }
                } else {
                    throw Exception("Export encoding failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = e.message ?: "Export failed"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetExport() {
        _uiState.update { it.copy(exportComplete = false, exportPath = null) }
    }
}
