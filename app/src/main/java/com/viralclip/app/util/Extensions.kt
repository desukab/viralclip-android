package com.viralclip.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Extensions {

    fun Long.formatDuration(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun Long.formatDurationShort(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun Float.toPercentageString(): String = "${(this * 100).toInt()}%"

    fun Float.toScoreColor(): Long = when {
        this >= 0.7f -> 0xFF10B981 // Green
        this >= 0.4f -> 0xFFF59E0B // Yellow
        else -> 0xFFEF4444 // Red
    }

    fun String.toUri(): Uri = Uri.parse(this)

    fun Context.getFileName(uri: Uri): String {
        var name = "video"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun Context.getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    fun Long.formatFileSize(): String {
        return when {
            this < 1024 -> "$this B"
            this < 1024 * 1024 -> "${this / 1024} KB"
            this < 1024 * 1024 * 1024 -> "${"%.1f".format(this / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(this / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    fun Long.formatDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(this))
    }

    fun Long.formatDateTime(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(this))
    }

    fun Long.formatRelativeDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - this
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> formatDate()
        }
    }

    fun PlatformPreset.maxDurationFormatted(): String {
        val minutes = maxDurationSeconds / 60
        val seconds = maxDurationSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun Context.getOutputDirectory(subDir: String = "ViralClip"): File {
        val dir = File(getExternalFilesDir(null), subDir)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun Context.getTempDirectory(): File {
        val dir = File(cacheDir, "processing")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun Context.getExportDirectory(): File {
        val dir = getOutputDirectory("Exports")
        return dir
    }

    fun Context.getThumbnailDirectory(): File {
        val dir = getOutputDirectory("Thumbnails")
        return dir
    }
}
