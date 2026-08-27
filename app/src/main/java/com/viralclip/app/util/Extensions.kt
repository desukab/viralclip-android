package com.viralclip.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.viralclip.app.domain.model.PlatformPreset
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Extensions {

    fun Long.formatDuration(): String = TimeFormatter.formatDuration(this)

    fun Long.formatDurationShort(): String = TimeFormatter.formatDurationShort(this)

    fun Long.formatDurationWithMs(): String = TimeFormatter.formatDurationWithMs(this)

    fun Long.formatSrtTimestamp(): String = TimeFormatter.formatSrtTimestamp(this)

    fun Long.formatVttTimestamp(): String = TimeFormatter.formatVttTimestamp(this)

    fun Int.secondsToTimestamp(): String = TimeFormatter.secondsToTimestamp(this)

    fun Int.secondsToDurationLabel(): String = TimeFormatter.secondsToDurationLabel(this)

    fun Float.toPercentageString(): String = "${(this * 100).toInt()}%"

    fun Float.toScoreColor(): Long = when {
        this >= 0.7f -> 0xFF10B981
        this >= 0.4f -> 0xFFF59E0B
        else -> 0xFFEF4444
    }

    fun String.toUri(): Uri = Uri.parse(this)

    fun Context.getFileName(uri: Uri): String = FileUtils.getFileName(this, uri)

    fun Context.getFileSize(uri: Uri): Long = FileUtils.getFileSize(this, uri)

    fun Context.getMimeType(uri: Uri): String = FileUtils.getMimeType(this, uri)

    fun Long.formatFileSize(): String = TimeFormatter.formatFileSize(this)

    fun Long.formatBitrate(): String = TimeFormatter.formatBitrate(this)

    fun Long.formatDate(pattern: String = "MMM dd, yyyy"): String = TimeFormatter.formatDate(this, pattern)

    fun Long.formatDateTime(pattern: String = "MMM dd, yyyy HH:mm"): String = TimeFormatter.formatDateTime(this, pattern)

    fun Long.formatRelativeDate(): String = TimeFormatter.formatRelativeDate(this)

    fun Long.formatProcessingTime(): String = TimeFormatter.formatProcessingTime(this)

    fun Long.formatSmartDate(): String = TimeFormatter.formatSmartDate(this)

    fun Long.isToday(): Boolean = TimeFormatter.isToday(this)

    fun Long.isYesterday(): Boolean = TimeFormatter.isYesterday(this)

    fun Long.estimateRemainingTime(progress: Float): Long = TimeFormatter.estimateRemainingTime(progress, this)

    fun PlatformPreset.maxDurationFormatted(): String {
        val minutes = maxDurationSeconds / 60
        val seconds = maxDurationSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    fun Context.getOutputDirectory(subDir: String = "ViralClip"): File = FileUtils.getOutputDirectory(this, subDir)

    fun Context.getTempDirectory(): File = FileUtils.getTempDirectory(this)

    fun Context.getExportDirectory(): File = FileUtils.getExportDirectory(this)

    fun Context.getThumbnailDirectory(): File = FileUtils.getThumbnailDirectory(this)

    fun Context.getCacheDirectory(subDir: String = "cache"): File = FileUtils.getCacheDirectory(this, subDir)

    fun Context.createTempFile(prefix: String, suffix: String = ".mp4"): File = FileUtils.createTempFile(this, prefix, suffix)

    fun Context.getAvailableStorageSpace(): Long = FileUtils.getAvailableStorageSpace(this)

    fun Context.getTotalStorageSpace(): Long = FileUtils.getTotalStorageSpace(this)

    fun Context.hasEnoughStorage(requiredBytes: Long): Boolean = FileUtils.hasEnoughStorage(this, requiredBytes)

    fun Context.getMediaDuration(uri: Uri): Long = FileUtils.getMediaDuration(this, uri)

    fun File.generateChecksum(): String = FileUtils.generateChecksum(this)

    fun sanitizeFileName(name: String): String = FileUtils.sanitizeFileName(name)

    fun generateUniqueFileName(baseName: String, extension: String, directory: File): File =
        FileUtils.generateUniqueFileName(baseName, extension, directory)

    fun Long.formatTimestamp(): String {
        val hours = TimeUnit.MILLISECONDS.toHours(this)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        val ms = (this % 1000) / 10
        return if (hours > 0) {
            String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, ms)
        } else {
            String.format("%d:%02d.%02d", minutes, seconds, ms)
        }
    }

    fun Float.clamp(min: Float, max: Float): Float = this.coerceIn(min, max)

    fun Int.clamp(min: Int, max: Int): Int = this.coerceIn(min, max)

    fun Long.clamp(min: Long, max: Long): Long = this.coerceIn(min, max)

    inline fun <T> List<T>.mapIndexedSafe(action: (index: Int, T) -> Unit): List<T> {
        return this.mapIndexed { index, item ->
            action(index, item)
            item
        }
    }

    fun <T> List<T>.takeRandom(count: Int): List<T> = shuffled().take(count)

    fun <T> List<T>.secondOrNull(): T? = getOrNull(1)

    fun <T> List<T>.lastOrNull(predicate: (T) -> Boolean): T? = reversed().find(predicate)

    inline fun <T> Iterable<T>.sumOf(selector: (T) -> Long): Long {
        var sum = 0L
        for (element in this) {
            sum += selector(element)
        }
        return sum
    }

    fun Long.Companion.random(min: Long, max: Long): Long {
        return (min..max).random()
    }

    fun Int.Companion.random(min: Int, max: Int): Int {
        return (min..max).random()
    }

    fun Float.Companion.random(min: Float, max: Float): Float {
        return (min..max).random().toFloat()
    }

    fun String.Companion.random(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    fun Boolean.toInt(): Int = if (this) 1 else 0

    fun Int.toBoolean(): Boolean = this != 0

    inline fun <T> withLock(lock: Any, block: () -> T): T {
        synchronized(lock) {
            return block()
        }
    }

    inline fun <T, R> T.letNotNull(block: (T) -> R): R? {
        return this.let(block)
    }

    fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    fun String.removeWhitespace(): String = replace(Regex("\\s+"), "")

    fun String.normalizeWhitespace(): String = replace(Regex("\\s+"), " ").trim()

    fun String.extractNumbers(): List<Int> = Regex("\\d+").findAll(this).map { it.value.toInt() }.toList()

    fun String.extractDecimals(): List<Float> = Regex("\\d+\\.?\\d*").findAll(this).map { it.value.toFloat() }.toList()

    fun String.isValidUrl(): Boolean = Regex("^https?://.*").matches(this)

    fun String.isValidEmail(): Boolean = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(this)

    fun String.truncate(maxLength: Int, suffix: String = "..."): String {
        return if (this.length > maxLength) {
            this.take(maxLength - suffix.length) + suffix
        } else {
            this
        }
    }

    fun String.repeat(count: Int, separator: String = ""): String {
        return (1..count).joinToString(separator) { this }
    }

    fun <K, V> Map<K, V>.merge(other: Map<K, V>): Map<K, V> {
        return this.toMutableMap().apply { putAll(other) }
    }

    fun <T> Collection<T>.groupByCount(): Map<T, Int> {
        return groupingBy { it }.eachCount()
    }

    fun <T> Collection<T>.mostCommon(): T? {
        return groupByCount().maxByOrNull { it.value }?.key
    }

    fun <T> Collection<T>.leastCommon(): T? {
        return groupByCount().minByOrNull { it.value }?.key
    }

    inline fun <reified T> Array<T>.toListSafe(): List<T> {
        return this.filterIsInstance<T>()
    }

    fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    fun String.hexToByteArray(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun Long.toFileSize(): String = TimeFormatter.formatFileSize(this)

    fun Float.toDecibel(): Float = 20 * kotlin.math.log10(this)

    fun Float.fromDecibel(): Float = kotlin.math.pow(10.0, this / 20.0).toFloat()

    fun Float.normalizeToDb(): Float = ((this + 60f) / 60f).coerceIn(0f, 1f)

    fun Float.denormalizeFromDb(): Float = (this * 60f) - 60f
}
