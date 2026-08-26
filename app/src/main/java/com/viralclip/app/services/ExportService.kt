package com.viralclip.app.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.viralclip.app.MainActivity
import com.viralclip.app.R
import com.viralclip.app.ViralClipApp
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

/**
 * Foreground service for exporting videos with all edits applied.
 */
@AndroidEntryPoint
class ExportService : Service() {

    @Inject lateinit var ffmpegProcessor: FFmpegProcessor

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var exportJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXPORT -> {
                val clipId = intent.getLongExtra(EXTRA_CLIP_ID, 0)
                val sourceUri = intent.getStringExtra(EXTRA_SOURCE_URI) ?: return START_NOT_STICKY
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: return START_NOT_STICKY
                val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
                val fps = intent.getIntExtra(EXTRA_FPS, 30)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
                startForeground(NOTIFICATION_ID, createNotification("Exporting…"))
                startExport(Uri.parse(sourceUri), outputPath, width, height, fps, bitrate, clipId)
            }
            ACTION_CANCEL -> {
                exportJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startExport(
        sourceUri: Uri,
        outputPath: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        clipId: Long
    ) {
        exportJob = serviceScope.launch {
            try {
                updateNotification("Exporting video…", 0)

                val success = ffmpegProcessor.exportVideo(
                    inputUri = sourceUri,
                    outputPath = outputPath,
                    width = width,
                    height = height,
                    bitrate = bitrate,
                    fps = fps,
                    onProgress = { progress ->
                        updateNotification("Exporting… ${(progress * 100).toInt()}%", (progress * 100).toInt())
                    }
                )

                if (success) {
                    sendBroadcast(Intent(ACTION_EXPORT_COMPLETE).apply {
                        putExtra(EXTRA_OUTPUT_PATH, outputPath)
                        putExtra(EXTRA_CLIP_ID, clipId)
                    })
                } else {
                    sendBroadcast(Intent(ACTION_EXPORT_ERROR).apply {
                        putExtra(EXTRA_ERROR_MESSAGE, "Export failed")
                    })
                }
            } catch (e: CancellationException) {
                // Cancelled
            } catch (e: Exception) {
                sendBroadcast(Intent(ACTION_EXPORT_ERROR).apply {
                    putExtra(EXTRA_ERROR_MESSAGE, e.message ?: "Export failed")
                })
            } finally {
                delay(1000)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ViralClipApp.CHANNEL_EXPORT)
            .setContentTitle("ViralClip Export")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val notification = createNotification(text).apply {
            setProgress(100, progress.coerceIn(0, 100), false)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_EXPORT = "com.viralclip.ACTION_EXPORT"
        const val ACTION_CANCEL = "com.viralclip.ACTION_CANCEL_EXPORT"
        const val ACTION_EXPORT_COMPLETE = "com.viralclip.EXPORT_COMPLETE"
        const val ACTION_EXPORT_ERROR = "com.viralclip.EXPORT_ERROR"
        const val EXTRA_CLIP_ID = "clip_id"
        const val EXTRA_SOURCE_URI = "source_uri"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_FPS = "fps"
        const val EXTRA_BITRATE = "bitrate"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        private const val NOTIFICATION_ID = 1002

        fun start(
            context: Context,
            clipId: Long,
            sourceUri: Uri,
            outputPath: String,
            width: Int = 1080,
            height: Int = 1920,
            fps: Int = 30,
            bitrate: Int = 8_000_000
        ) {
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_EXPORT
                putExtra(EXTRA_CLIP_ID, clipId)
                putExtra(EXTRA_SOURCE_URI, sourceUri.toString())
                putExtra(EXTRA_OUTPUT_PATH, outputPath)
                putExtra(EXTRA_WIDTH, width)
                putExtra(EXTRA_HEIGHT, height)
                putExtra(EXTRA_FPS, fps)
                putExtra(EXTRA_BITRATE, bitrate)
            }
            context.startForegroundService(intent)
        }
    }
}
