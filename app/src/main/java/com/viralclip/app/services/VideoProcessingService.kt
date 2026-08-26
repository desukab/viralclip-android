package com.viralclip.app.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.viralclip.app.MainActivity
import com.viralclip.app.R
import com.viralclip.app.ViralClipApp
import com.viralclip.app.domain.model.ProcessingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Foreground service for processing videos in the background.
 * Keeps the processing alive even when the app is in background.
 */
@AndroidEntryPoint
class VideoProcessingService : Service() {

    @Inject lateinit var pipeline: VideoProcessingPipeline

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var processingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PROCESS -> {
                val videoUriString = intent.getStringExtra(EXTRA_VIDEO_URI) ?: return START_NOT_STICKY
                val videoUri = Uri.parse(videoUriString)
                startForeground(NOTIFICATION_ID, createNotification("Preparing…"))
                startProcessing(videoUri)
            }
            ACTION_CANCEL -> {
                processingJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startProcessing(videoUri: Uri) {
        processingJob = serviceScope.launch {
            try {
                // Observe pipeline state for notification updates
                launch {
                    pipeline.state.collectLatest { state ->
                        val (title, progress) = when (state) {
                            is ProcessingState.Analyzing -> state.message to (state.progress * 100).toInt()
                            is ProcessingState.Transcribing -> "Transcribing audio…" to (state.progress * 100).toInt()
                            is ProcessingState.DetectingFaces -> "Detecting faces…" to (state.progress * 100).toInt()
                            is ProcessingState.ScoringVirality -> "Scoring virality…" to (state.progress * 100).toInt()
                            is ProcessingState.GeneratingClips -> "Generating clips…" to (state.progress * 100).toInt()
                            is ProcessingState.ApplyingCaptions -> "Applying captions…" to (state.progress * 100).toInt()
                            is ProcessingState.Exporting -> "Exporting…" to (state.progress * 100).toInt()
                            is ProcessingState.Error -> "Error: ${state.message}" to 0
                            ProcessingState.Complete -> "Complete!" to 100
                            ProcessingState.Idle -> "Preparing…" to 0
                        }
                        updateNotification(title, progress)
                    }
                }

                // Run the pipeline
                val result = pipeline.processVideo(videoUri, this@VideoProcessingService)

                // Send broadcast with results
                sendBroadcast(Intent(ACTION_PROCESSING_COMPLETE).apply {
                    putExtra(EXTRA_RESULT_CLIPS_COUNT, result.generatedClips.size)
                    putExtra(EXTRA_RESULT_VIRALITY_SCORE, result.viralityResult.overallVideoScore)
                })

            } catch (e: CancellationException) {
                // Processing cancelled
            } catch (e: Exception) {
                sendBroadcast(Intent(ACTION_PROCESSING_ERROR).apply {
                    putExtra(EXTRA_ERROR_MESSAGE, e.message ?: "Unknown error")
                })
            } finally {
                delay(1000) // Brief delay before stopping
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VideoProcessingService::class.java).apply {
                action = ACTION_CANCEL
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ViralClipApp.CHANNEL_PROCESSING)
            .setContentTitle("ViralClip")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_cancel, "Cancel", cancelIntent)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this, 0,
            Intent(this, VideoProcessingService::class.java).apply {
                action = ACTION_CANCEL
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ViralClipApp.CHANNEL_PROCESSING)
            .setContentTitle("ViralClip")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_cancel, "Cancel", cancelIntent)
            .setOngoing(true)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_PROCESS = "com.viralclip.ACTION_PROCESS"
        const val ACTION_CANCEL = "com.viralclip.ACTION_CANCEL"
        const val ACTION_PROCESSING_COMPLETE = "com.viralclip.PROCESSING_COMPLETE"
        const val ACTION_PROCESSING_ERROR = "com.viralclip.PROCESSING_ERROR"
        const val EXTRA_VIDEO_URI = "video_uri"
        const val EXTRA_RESULT_CLIPS_COUNT = "clips_count"
        const val EXTRA_RESULT_VIRALITY_SCORE = "virality_score"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, videoUri: Uri) {
            val intent = Intent(context, VideoProcessingService::class.java).apply {
                action = ACTION_PROCESS
                putExtra(EXTRA_VIDEO_URI, videoUri.toString())
            }
            context.startForegroundService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, VideoProcessingService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
