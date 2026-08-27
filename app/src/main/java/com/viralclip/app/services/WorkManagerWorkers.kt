package com.viralclip.app.services

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.viralclip.app.data.preferences.UserPreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltWorker
class VideoProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: VideoProcessingPipeline,
    private val preferences: UserPreferencesManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val videoUriString = inputData.getString(KEY_VIDEO_URI)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing video URI"))
        val videoUri = Uri.parse(videoUriString)

        return try {
            // Observe pipeline state for progress reporting while processing.
            // Use a job that completes when a terminal state is reached, so doWork
            // doesn't race with the collector.
            val stateJob = launch {
                pipeline.state.collectLatest { state ->
                    val (msg, progress) = when (state) {
                        is com.viralclip.app.domain.model.ProcessingState.Analyzing -> state.message to state.progress
                        is com.viralclip.app.domain.model.ProcessingState.Transcribing -> "Transcribing…" to state.progress
                        is com.viralclip.app.domain.model.ProcessingState.DetectingFaces -> "Detecting faces…" to state.progress
                        is com.viralclip.app.domain.model.ProcessingState.ScoringVirality -> "Scoring virality…" to state.progress
                        is com.viralclip.app.domain.model.ProcessingState.GeneratingClips -> "Generating clips…" to state.progress
                        is com.viralclip.app.domain.model.ProcessingState.ApplyingCaptions -> "Applying captions…" to state.progress
                        is com.viralclip.app.domain.model.ProcessingState.Error -> state.message to 0f
                        com.viralclip.app.domain.model.ProcessingState.Complete -> "Complete" to 1f
                        com.viralclip.app.domain.model.ProcessingState.Idle -> "Preparing" to 0f
                        is com.viralclip.app.domain.model.ProcessingState.Exporting -> "Exporting" to state.progress
                    }
                    setProgress(workDataOf(KEY_PROGRESS to progress, KEY_STATUS_MSG to msg))
                }
            }

            val result = pipeline.processVideo(videoUri, applicationContext)
            stateJob.cancel()

            preferences.incrementProcessedVideos()
            pipeline.reset()

            Result.success(
                workDataOf(
                    KEY_CLIPS_COUNT to result.generatedClips.size,
                    KEY_VIRALITY_SCORE to result.viralityResult.overallVideoScore
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            Result.failure(workDataOf(KEY_ERROR to "Cancelled"))
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS_MSG = "status_msg"
        const val KEY_CLIPS_COUNT = "clips_count"
        const val KEY_VIRALITY_SCORE = "virality_score"
        const val KEY_ERROR = "error"
        const val UNIQUE_NAME = "video_processing"

        fun buildRequest(videoUri: Uri): OneTimeWorkRequest {
            val data = workDataOf(KEY_VIDEO_URI to videoUri.toString())
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresStorageNotLow(true)
                .build()

            return OneTimeWorkRequestBuilder<VideoProcessingWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("processing")
                .build()
        }
    }
}

@HiltWorker
class ExportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val outputPath = inputData.getString(KEY_OUTPUT_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing output path"))
        val clipId = inputData.getLong(KEY_CLIP_ID, -1L)
        return try {
            val file = java.io.File(outputPath)
            // Validate the output is a real, playable video: non-empty + valid container
            if (file.exists() && file.length() > 0) {
                val valid = android.media.MediaMetadataRetriever().use { mmr ->
                    runCatching {
                        mmr.setDataSource(file.absolutePath)
                        mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() != null
                    }.getOrDefault(false)
                }
                if (valid) {
                    preferences.incrementExportedClips()
                    Result.success(
                        workDataOf(
                            KEY_OUTPUT_PATH to outputPath,
                            KEY_CLIP_ID to clipId,
                            KEY_FILE_SIZE to file.length()
                        )
                    )
                } else {
                    Result.failure(workDataOf(KEY_ERROR to "Output file invalid or not playable"))
                }
            } else {
                Result.failure(workDataOf(KEY_ERROR to "Output file missing or empty"))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Export failed")))
        }
    }

    companion object {
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_CLIP_ID = "clip_id"
        const val KEY_FILE_SIZE = "file_size"
        const val KEY_ERROR = "error"
        const val UNIQUE_NAME = "video_export"

        fun buildRequest(outputPath: String, clipId: Long): OneTimeWorkRequest {
            val data = workDataOf(
                KEY_OUTPUT_PATH to outputPath,
                KEY_CLIP_ID to clipId
            )
            return OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag("export")
                .build()
        }
    }
}

@HiltWorker
class CacheCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            applicationContext.cacheDir.listFiles()?.forEach { file ->
                // Skip the currently-writing temp file and non-cache ISOs
                if (file.name.endsWith(".tmp") || file.name.endsWith(".downloading")) return@forEach
                if (file.lastModified() < cutoff) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "cache_cleanup"

        fun buildPeriodicRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
            return PeriodicWorkRequestBuilder<CacheCleanupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag("maintenance")
                .build()
        }
    }
}
