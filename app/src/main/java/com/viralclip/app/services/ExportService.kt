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
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.data.database.dao.ClipDao
import com.viralclip.app.data.database.dao.ExportedAssetDao
import com.viralclip.app.data.database.entities.ExportedAssetEntity
import com.viralclip.app.data.preferences.UserPreferencesManager
import com.viralclip.app.util.FileStorageManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ExportService : Service() {

    @Inject lateinit var ffmpegProcessor: FFmpegProcessor
    @Inject lateinit var clipDao: ClipDao
    @Inject lateinit var exportedAssetDao: ExportedAssetDao
    @Inject lateinit var preferences: UserPreferencesManager
    @Inject lateinit var storageManager: FileStorageManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, _ -> })
    private var exportJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundType()
    }

    private fun ensureForegroundType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val field = Service::class.java.getDeclaredField("mForegroundServiceType")
                field.isAccessible = true
            } catch (_: Exception) { }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXPORT -> handleExportIntent(intent)
            ACTION_CANCEL -> cancelExport()
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification("Preparing…", 0, true))
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleExportIntent(intent: Intent) {
        val sourceUri = intent.getStringExtra(EXTRA_SOURCE_URI)
        val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
        val clipId = intent.getLongExtra(EXTRA_CLIP_ID, 0L)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: storageManager.generateExportFileName()
        val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, 0L)

        if (sourceUri == null) {
            startForeground(NOTIFICATION_ID, buildNotification("Error: missing source", 0, false))
            sendErrorBroadcast("Missing source URI", clipId)
            serviceScope.launch {
                delaySafely(500)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return
        }

        val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
        val fps = intent.getIntExtra(EXTRA_FPS, 30)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 8_000_000)
        val includeCaptions = intent.getBooleanExtra(EXTRA_INCLUDE_CAPTIONS, true)

        val finalOutputPath = outputPath ?: storageManager.getOutputPath(fileName)

        startForeground(NOTIFICATION_ID, buildNotification("Preparing export…", 0, true))
        startExport(
            sourceUri = Uri.parse(sourceUri),
            outputPath = finalOutputPath,
            width = width,
            height = height,
            fps = fps,
            bitrate = bitrate,
            clipId = clipId,
            projectId = projectId,
                    fileName = fileForPath(finalOutputPath).name,
            includeCaptions = includeCaptions
        )
    }

    private fun cancelExport() {
        exportJob?.cancel()
        startForeground(NOTIFICATION_ID, buildNotification("Cancelling…", 0, true))
        sendBroadcast(Intent(ACTION_EXPORT_CANCELLED))
        serviceScope.launch {
            delaySafely(300)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startExport(
        sourceUri: Uri,
        outputPath: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        clipId: Long,
        projectId: Long,
        fileName: String,
        includeCaptions: Boolean
    ) {
        exportJob?.cancel()
        exportJob = serviceScope.launch {
            try {
                updateNotification("Exporting video…", 0)

                val outputFile = java.io.File(outputPath)
                outputFile.parentFile?.mkdirs()
                if (outputFile.exists()) outputFile.delete()

                if (clipId > 0) clipDao.updateExportProgress(clipId, 0f)

                val success = ffmpegProcessor.exportVideo(
                    inputUri = sourceUri,
                    outputPath = outputPath,
                    width = width,
                    height = height,
                    bitrate = bitrate,
                    fps = fps,
                    onProgress = { progress ->
                        val pct = (progress * 100).toInt().coerceIn(0, 100)
                        updateNotification("Exporting… $pct%", pct)
                        if (clipId > 0) {
                            serviceScope.launch { clipDao.updateExportProgress(clipId, progress) }
                        }
                    }
                )

                if (success && outputFile.exists() && outputFile.length() > 0) {
                    val mimeType = when (fileName.substringAfterLast('.', "").lowercase()) {
                        "mov" -> "video/quicktime"
                        "webm" -> "video/webm"
                        else -> "video/mp4"
                    }
                    val mediaUri = try {
                        storageManager.saveToMediaStore(outputFile, mimeType)
                    } catch (_: Exception) {
                        null
                    }

                    if (clipId > 0) {
                        clipDao.markExported(clipId, outputPath)
                    }
                    if (projectId > 0) {
                        exportedAssetDao.insert(
                            ExportedAssetEntity(
                                projectId = projectId,
                                clipId = if (clipId > 0) clipId else null,
                                filePath = outputPath,
                                fileName = fileName,
                                mimeType = mimeType,
                                sizeBytes = outputFile.length(),
                                width = width,
                                height = height,
                                durationMs = 0
                            )
                        )
                    }
                    preferences.incrementExportedClips()

                    updateNotification("Export complete", 100)
                    sendCompleteBroadcast(outputPath, clipId, mediaUri?.toString())
                } else {
                    val msg = if (outputFile.exists() && outputFile.length() == 0L) "Empty output file" else "Export failed"
                    if (outputFile.exists()) outputFile.delete()
                    sendErrorBroadcast(msg, clipId)
                }
            } catch (e: CancellationException) {
                sendBroadcast(Intent(ACTION_EXPORT_CANCELLED))
            } catch (e: Exception) {
                sendErrorBroadcast(e.message ?: "Unknown error", clipId)
            } finally {
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

    private fun sendCompleteBroadcast(outputPath: String, clipId: Long, mediaUri: String?) {
        val intent = Intent(ACTION_EXPORT_COMPLETE).apply {
            putExtra(EXTRA_OUTPUT_PATH, outputPath)
            putExtra(EXTRA_CLIP_ID, clipId)
            if (mediaUri != null) putExtra(EXTRA_MEDIA_URI, mediaUri)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun sendErrorBroadcast(message: String, clipId: Long) {
        val intent = Intent(ACTION_EXPORT_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, message)
            putExtra(EXTRA_CLIP_ID, clipId)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(contentText: String, progress: Int, indeterminate: Boolean): Notification {
        val contentPi = PendingIntent.getActivity(
            this, REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelPi = PendingIntent.getService(
            this, REQUEST_CANCEL,
            Intent(this, ExportService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, ViralClipApp.CHANNEL_EXPORT)
            .setContentTitle("ViralClip Export")
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
        exportJob?.cancel()
        serviceScope.cancel()
    }

    override fun onTimeout(p: Int) {
        super.onTimeout(p)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (exportJob?.isActive == true) {
            exportJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    companion object {
        const val ACTION_EXPORT = "com.viralclip.ACTION_EXPORT"
        const val ACTION_CANCEL = "com.viralclip.ACTION_CANCEL_EXPORT"
        const val ACTION_EXPORT_COMPLETE = "com.viralclip.EXPORT_COMPLETE"
        const val ACTION_EXPORT_ERROR = "com.viralclip.EXPORT_ERROR"
        const val ACTION_EXPORT_CANCELLED = "com.viralclip.EXPORT_CANCELLED"
        const val EXTRA_CLIP_ID = "clip_id"
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_SOURCE_URI = "source_uri"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_WIDTH = "width"
        const val EXTRA_HEIGHT = "height"
        const val EXTRA_FPS = "fps"
        const val EXTRA_BITRATE = "bitrate"
        const val EXTRA_INCLUDE_CAPTIONS = "include_captions"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_MEDIA_URI = "media_uri"
        private const val NOTIFICATION_ID = 1002
        private const val REQUEST_OPEN = 2001
        private const val REQUEST_CANCEL = 2002

        fun start(
            context: Context,
            clipId: Long,
            sourceUri: Uri,
            outputPath: String? = null,
            fileName: String? = null,
            projectId: Long = 0L,
            width: Int = 1080,
            height: Int = 1920,
            fps: Int = 30,
            bitrate: Int = 8_000_000,
            includeCaptions: Boolean = true
        ) {
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_EXPORT
                putExtra(EXTRA_CLIP_ID, clipId)
                putExtra(EXTRA_PROJECT_ID, projectId)
                putExtra(EXTRA_SOURCE_URI, sourceUri.toString())
                if (outputPath != null) putExtra(EXTRA_OUTPUT_PATH, outputPath)
                if (fileName != null) putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_WIDTH, width)
                putExtra(EXTRA_HEIGHT, height)
                putExtra(EXTRA_FPS, fps)
                putExtra(EXTRA_BITRATE, bitrate)
                putExtra(EXTRA_INCLUDE_CAPTIONS, includeCaptions)
            }
            context.startForegroundService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, ExportService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}

private fun fileForPath(name: String): java.io.File = java.io.File(name)
