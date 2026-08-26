package com.viralclip.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ViralClipApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val processingChannel = NotificationChannel(
                CHANNEL_PROCESSING,
                "Video Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of video processing"
                setShowBadge(false)
            }

            val exportChannel = NotificationChannel(
                CHANNEL_EXPORT,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows export progress"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Completion and error alerts"
            }

            manager.createNotificationChannels(
                listOf(processingChannel, exportChannel, alertChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_PROCESSING = "video_processing"
        const val CHANNEL_EXPORT = "video_export"
        const val CHANNEL_ALERTS = "alerts"
    }
}
