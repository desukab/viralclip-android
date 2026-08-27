package com.viralclip.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.collection.LruCache
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    private const val DEFAULT_THUMBNAIL_WIDTH = 320
    private const val DEFAULT_THUMBNAIL_HEIGHT = 320
    private const val DEFAULT_THUMBNAIL_QUALITY = 85

    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(calculateCacheSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private fun calculateCacheSize(): Int {
        val maxMemory = Runtime.getRuntime().maxMemory().toInt()
        return (maxMemory / 8) / 1024
    }

    fun putBitmapInCache(key: String, bitmap: Bitmap) {
        bitmapCache.put(key, bitmap)
    }

    fun getBitmapFromCache(key: String): Bitmap? {
        return bitmapCache.get(key)
    }

    fun clearCache() {
        bitmapCache.evictAll()
    }

    fun cacheSize(): Int {
        return bitmapCache.size()
    }

    fun maxCacheSize(): Int {
        return bitmapCache.maxSize()
    }

    fun Context.loadBitmap(
        uri: Uri,
        reqWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        reqHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? {
        val cacheKey = "${uri.toString()}_${reqWidth}x$reqHeight"
        bitmapCache.get(cacheKey)?.let { return it }

        val bitmap = decodeSampledBitmapFromUri(uri, reqWidth, reqHeight) ?: return null
        bitmapCache.put(cacheKey, bitmap)
        return bitmap
    }

    fun Context.decodeSampledBitmapFromUri(
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inMutable = false

        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun extractVideoFrame(
        file: File,
        timeMs: Long,
        maxWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        maxHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val fullBitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            fullBitmap?.let { scaleBitmap(it, maxWidth, maxHeight) }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun extractVideoFrame(
        context: Context,
        uri: Uri,
        timeMs: Long,
        maxWidth: Int = DEFAULT_THUMBNAIL_WIDTH,
        maxHeight: Int = DEFAULT_THUMBNAIL_HEIGHT
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val fullBitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            fullBitmap?.let { scaleBitmap(it, maxWidth, maxHeight) }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth && height <= maxHeight) return bitmap

        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun cropToAspectRatio(bitmap: Bitmap, targetRatio: Float): Bitmap {
        val currentRatio = bitmap.width.toFloat() / bitmap.height
        return if (kotlin.math.abs(currentRatio - targetRatio) < 0.01f) {
            bitmap
        } else {
            val width: Int
            val height: Int
            val xOffset: Int
            val yOffset: Int
            if (currentRatio > targetRatio) {
                height = bitmap.height
                width = (bitmap.height * targetRatio).toInt()
                xOffset = (bitmap.width - width) / 2
                yOffset = 0
            } else {
                width = bitmap.width
                height = (bitmap.width / targetRatio).toInt()
                xOffset = 0
                yOffset = (bitmap.height - height) / 2
            }
            Bitmap.createBitmap(bitmap, xOffset, yOffset, width, height)
        }
    }

    fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    fun applyBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        val matrix = ColorMatrix()
        matrix.set(floatArrayOf(
            1f, 0f, 0f, 0f, brightness * 255f,
            0f, 1f, 0f, 0f, brightness * 255f,
            0f, 0f, 1f, 0f, brightness * 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, matrix)
    }

    fun applyContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val translate = (1f - contrast) * 128f
        val matrix = ColorMatrix()
        matrix.set(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        return applyColorMatrix(bitmap, matrix)
    }

    fun applySaturation(bitmap: Bitmap, saturation: Float): Bitmap {
        val matrix = ColorMatrix()
        matrix.setSaturation(saturation)
        return applyColorMatrix(bitmap, matrix)
    }

    fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        return applyColorMatrix(bitmap, matrix)
    }

    fun applySepia(bitmap: Bitmap): Bitmap {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val sepiaMatrix = ColorMatrix()
        sepiaMatrix.set(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(sepiaMatrix)
        return applyColorMatrix(bitmap, matrix)
    }

    fun applyVignette(bitmap: Bitmap, intensity: Float): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = kotlin.math.hypot(centerX, centerY)
        val gradient = android.graphics.RadialGradient(
            centerX, centerY, radius,
            intArrayOf(0x00000000, 0xCC000000.toInt()),
            floatArrayOf(0.7f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            shader = gradient
            alpha = (intensity * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return output
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun flipBitmap(bitmap: Bitmap, horizontal: Boolean = false, vertical: Boolean = false): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f)
            if (vertical) postScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun saveBitmapToFile(
        bitmap: Bitmap,
        file: File,
        quality: Int = DEFAULT_THUMBNAIL_QUALITY,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): File {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(format, quality.coerceIn(1, 100), out)
            out.flush()
        }
        return file
    }

    fun Context.saveBitmapAsThumbnail(bitmap: Bitmap, baseName: String = "thumb"): File {
        val dir = with(FileUtils) { this@saveBitmapAsThumbnail.getThumbnailDirectory() }
        val file = File(dir, "${baseName}_${UUID.randomUUID()}.jpg")
        return saveBitmapToFile(bitmap, file)
    }

    fun calculateAverageBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 10
        var totalBrightness = 0L
        var sampleCount = 0

        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
                sampleCount++
            }
        }
        return if (sampleCount > 0) totalBrightness.toFloat() / sampleCount / 255f else 0.5f
    }

    fun calculateMotionScore(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) return 0f
        val width = bitmap1.width
        val height = bitmap1.height
        val sampleSize = 8
        var totalDiff = 0L
        var sampleCount = 0

        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val p1 = bitmap1.getPixel(x, y)
                val p2 = bitmap2.getPixel(x, y)
                val r1 = (p1 shr 16) and 0xFF
                val g1 = (p1 shr 8) and 0xFF
                val b1 = p1 and 0xFF
                val r2 = (p2 shr 16) and 0xFF
                val g2 = (p2 shr 8) and 0xFF
                val b2 = p2 and 0xFF
                val diff = kotlin.math.abs(r1 - r2) + kotlin.math.abs(g1 - g2) + kotlin.math.abs(b1 - b2)
                totalDiff += diff
                sampleCount++
            }
        }
        return if (sampleCount > 0) (totalDiff.toFloat() / sampleCount) / 255f else 0f
    }

    fun blendBitmaps(bitmap1: Bitmap, bitmap2: Bitmap, alpha: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap1.width, bitmap1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(bitmap1, 0f, 0f, null)
        canvas.drawBitmap(bitmap2, 0f, 0f, paint)
        return output
    }
}
