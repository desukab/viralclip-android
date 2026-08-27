package com.viralclip.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeFormatter {

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

    fun Long.formatDurationWithMs(): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(this)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val ms = (this % 1000) / 10
        return String.format("%d:%02d.%02d", minutes, seconds, ms)
    }

    fun Long.formatSrtTimestamp(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        val ms = this % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
    }

    fun Long.formatVttTimestamp(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        val ms = this % 1000
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, ms)
    }

    fun Int.secondsToTimestamp(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        val seconds = this % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun Int.secondsToDurationLabel(): String {
        val hours = this / 3600
        val minutes = (this % 3600) / 60
        val seconds = this % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    fun Long.formatFileSize(): String {
        if (this <= 0L) return "0 B"
        return when {
            this < 1024 -> "$this B"
            this < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", this / 1024.0)
            this < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", this / (1024.0 * 1024.0))
            else -> String.format(Locale.US, "%.2f GB", this / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun Long.formatBitrate(): String {
        return when {
            this < 1_000 -> "$this bps"
            this < 1_000_000 -> String.format(Locale.US, "%.1f Kbps", this / 1000.0)
            else -> String.format(Locale.US, "%.2f Mbps", this / 1_000_000.0)
        }
    }

    fun Long.formatDate(pattern: String = "MMM dd, yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(this))
    }

    fun Long.formatDateTime(pattern: String = "MMM dd, yyyy HH:mm"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(this))
    }

    fun Long.formatTime(pattern: String = "HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(this))
    }

    fun Long.formatRelativeDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - this
        return when {
            diff < TimeUnit.SECONDS.toMillis(30) -> "Just now"
            diff < TimeUnit.MINUTES.toMillis(1) -> "${TimeUnit.MILLISECONDS.toSeconds(diff)}s ago"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            diff < TimeUnit.DAYS.toMillis(30) -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
                "${weeks}w ago"
            }
            diff < TimeUnit.DAYS.toMillis(365) -> {
                val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
                "${months}mo ago"
            }
            else -> {
                val years = TimeUnit.MILLISECONDS.toDays(diff) / 365
                "${years}y ago"
            }
        }
    }

    fun Long.formatProcessingTime(): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        return when {
            hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    fun Long.isToday(): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        calendar.timeInMillis = this
        val date = calendar.timeInMillis
        val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return todayFormat.format(today) == todayFormat.format(date)
    }

    fun Long.isYesterday(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.timeInMillis
        val yesterdayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return yesterdayFormat.format(yesterday) == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(this))
    }

    fun Long.formatSmartDate(): String {
        return when {
            isToday() -> "Today, " + formatTime("HH:mm")
            isYesterday() -> "Yesterday, " + formatTime("HH:mm")
            else -> formatDate("MMM dd, yyyy")
        }
    }

    fun formatTimestampFromSeconds(hours: Int, minutes: Int, seconds: Int): Long {
        return TimeUnit.HOURS.toMillis(hours.toLong()) +
                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                TimeUnit.SECONDS.toMillis(seconds.toLong())
    }

    fun estimateRemainingTime(progress: Float, elapsedMs: Long): Long {
        if (progress <= 0f || progress >= 1f) return 0L
        val totalEstimated = (elapsedMs / progress).toLong()
        return (totalEstimated - elapsedMs).coerceAtLeast(0L)
    }
}
