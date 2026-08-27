package com.viralclip.app.services

import android.app.Notification
import android.app.NotificationManager
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
import com.viralclip.app.data.preferences.UserPreferencesManager
import com.viralclip.app.domain.model.ProcessingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class VideoProcessingService : Service() {

    @Inject lateinit var pipeline: VideoProcessingPipeline
    @Inject lateinit var preferences: UserPreferencesManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + serviceExceptionHandler)
    private var processingJob: Job? = null

    private val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        sendErrorBroadcast(throwable.message ?: "Processing error")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundType()
    }

    private fun ensureForegroundType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                Service::class.java.getDeclaredField("mForegroundServiceType").let {
                    it.isAccessible = true
                }
            } catch (_: Exception) { }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PROCESS -> {
                val videoUriString = intent.getStringExtra(EXTRA_VIDEO_URI)
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)

                if (videoUriString == null) {
                    startForeground(NOTIFICATION_ID, buildNotification("Error: No video specified", 0, false))
                    sendErrorBroadcast("Missing video URI")
                    delaySafely(1000)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForeground(NOTIFICATION_ID, buildNotification("Preparing…", 0, true))
                startProcessing(Uri.parse(videoUriString), projectId)
            }
            ACTION_CANCEL -> cancelProcessing()
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Idle", 0, false))
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelProcessing() {
        processingJob?.cancel()
        pipeline.reset()
        sendBroadcast(Intent(ACTION_PROCESSING_CANCELLED))
        delaySafely(300)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startProcessing(videoUri: Uri, projectId: Long) {
        processingJob?.cancel()
        processingJob = serviceScope.launch {
            val stateObserverJob = launch {
                pipeline.state.collectLatest { state ->
                    val (text, progress) = when (state) {
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
                    updateNotification(text, progress)
                }
            }

            try {
                val result = pipeline.processVideo(videoUri, this@VideoProcessingService)
                preferences.incrementProcessedVideos()

                sendBroadcast(Intent(ACTION_PROCESSING_COMPLETE).apply {
                    putExtra(EXTRA_RESULT_CLIPS_COUNT, result.generatedClips.size)
                    putExtra(EXTRA_RESULT_VIRALITY_SCORE, result.viralityResult.overallVideoScore)
                    putExtra(EXTRA_PROJECT_ID, projectId)
                })

            } catch (e: CancellationException) {
                pipeline.reset()
                sendBroadcast(Intent(ACTION_PROCESSING_CANCELLED))
            } catch (e: Exception) {
                pipeline.reset()
                sendErrorBroadcast(e.message ?: "Unknown error")
            } finally {
                stateObserverJob.cancel()
                preferences.incrementProcessedVideos()
                delaySafely(500)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun delaySafely(ms: Long) {
        try {
            delay(ms)
        } catch (_: CancellationException) { }
    }

    private fun sendErrorBroadcast(message: String) {
        sendBroadcast(Intent(ACTION_PROCESSING_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, message)
        })
    }

    private fun buildNotification(contentText: String, progress: Int, indeterminate: Boolean): Notification {
        val contentPi = PendingIntent.getActivity(
            this, REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cancelPi = PendingIntent.getService(
            this, REQUEST_CANCEL,
            Intent(this, VideoProcessingService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, ViralClipApp.CHANNEL_PROCESSING)
            .setContentTitle("ViralClip")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, indeterminate)
            .addAction(R.drawable.ic_cancel, "Cancel", cancelPi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress, false))
    }

    override fun onDestroy() {
        super.onDestroy()
        processingJob?.cancel()
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (processingJob?.isActive == true) {
            processingJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    companion object {
        const val ACTION_PROCESS = "com.viralclip.ACTION_PROCESS"
        const val ACTION_CANCEL = "com.viralclip.ACTION_CANCEL"
        const val ACTION_PROCESSING_COMPLETE = "com.viralclip.PROCESSING_COMPLETE"
        const val ACTION_PROCESSING_ERROR = "com.viralclip.PROCESSING_ERROR"
        const val ACTION_PROCESSING_CANCELLED = "com.viralclip.PROCESSING_CANCELLED"
        const val EXTRA_VIDEO_URI = "video_uri"
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_RESULT_CLIPS_COUNT = "clips_count"
        const val EXTRA_RESULT_VIRALITY_SCORE = "virality_score"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN = 1001
        private const val REQUEST_CANCEL = 1002

        fun start(context: Context, videoUri: Uri, projectId: Long = 0L) {
            val intent = Intent(context, VideoProcessingService::class.java).apply {
                action = ACTION_PROCESS
                putExtra(EXTRA_VIDEO_URI, videoUri.toString())
                putExtra(EXTRA_PROJECT_ID, projectId)
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
