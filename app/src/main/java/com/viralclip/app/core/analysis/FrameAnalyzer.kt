package com.viralclip.app.core.analysis

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.graphics.Bitmap
import android.graphics.Color
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

/**
 * Frame-by-frame video analysis engine.
 * Analyzes brightness, motion, scene changes, and engagement metrics.
 *
 * All analysis operates on pre-downscaled frames (360px) to minimize memory usage.
 */
@Singleton
class FrameAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ffmpegProcessor: FFmpegProcessor
) {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    /**
     * Analyze frames and produce per-frame metrics.
     * Expects pre-downscaled frames from FFmpegProcessor.extractFrames().
     */
    suspend fun analyzeFrames(
        frames: List<Pair<Long, Bitmap>>
    ): List<FrameAnalysis> = withContext(Dispatchers.Default) {
        _progress.value = 0f
        val analyses = mutableListOf<FrameAnalysis>()

        for ((index, pair) in frames.withIndex()) {
            val (timestampMs, bitmap) = pair

            try {
                if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) continue

                val brightness = analyzeBrightness(bitmap)
                val motionScore = if (index > 0 && !frames[index - 1].second.isRecycled) {
                    calculateMotion(bitmap, frames[index - 1].second)
                } else 0f

                val sceneType = classifyScene(brightness, motionScore)
                val engagement = calculateEngagementScore(brightness, motionScore)

                analyses.add(
                    FrameAnalysis(
                        timestampMs = timestampMs,
                        brightness = brightness,
                        motionScore = motionScore,
                        faceCount = 0,
                        facePositions = emptyList(),
                        sceneType = sceneType,
                        speechDetected = brightness > 0.2f,
                        engagementScore = engagement
                    )
                )
            } catch (_: Exception) {
                // Bitmap may be recycled or invalid, skip this frame
            }
            _progress.value = (index + 1).toFloat() / frames.size
        }

        _progress.value = 1f
        analyses
    }

    /**
     * Detect scene changes / cuts in video using histogram comparison.
     */
    suspend fun detectSceneChanges(
        frames: List<Pair<Long, Bitmap>>,
        threshold: Float = 0.35f
    ): List<Long> = withContext(Dispatchers.Default) {
        val sceneChanges = mutableListOf<Long>()
        var prevHistogram: FloatArray? = null

        for ((_, pair) in frames.withIndex()) {
            val (timestampMs, bitmap) = pair

            try {
                if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) continue

                val histogram = computeHistogram(bitmap)

                if (prevHistogram != null) {
                    val diff = histogramDifference(prevHistogram, histogram)
                    if (diff > threshold) {
                        sceneChanges.add(timestampMs)
                    }
                }

                prevHistogram = histogram
            } catch (_: Exception) {
                // Skip invalid frames
            }
        }

        sceneChanges
    }

    /**
     * Find the most engaging moments in video.
     */
    suspend fun findEngagingMoments(
        frames: List<Pair<Long, Bitmap>>,
        windowSize: Int = 10
    ): List<Pair<Long, Float>> = withContext(Dispatchers.Default) {
        val moments = mutableListOf<Pair<Long, Float>>()
        val analyses = analyzeFrames(frames)

        for (i in analyses.indices) {
            val windowEnd = minOf(i + windowSize, analyses.size)
            val window = analyses.subList(i, windowEnd)
            val avgEngagement = window.map { it.engagementScore }.average().toFloat()
            moments.add(analyses[i].timestampMs to avgEngagement)
        }

        moments.sortedByDescending { it.second }.take(10)
    }

    /**
     * Analyze brightness using sampled pixels for performance.
     * Operates on downscaled frames (360px) so full pixel scan is fine.
     */
    private fun analyzeBrightness(bitmap: Bitmap): Float {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return 0.5f

        // Use stride sampling for speed on even downscaled frames
        val stride = maxOf(1, minOf(bitmap.width, bitmap.height) / 30)
        var totalBrightness = 0L
        var count = 0

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        try {
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        } catch (_: Exception) {
            return 0.5f
        }

        for (y in 0 until h step stride) {
            for (x in 0 until w step stride) {
                val pixel = pixels[y * w + x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                totalBrightness += (r * 0.299 + g * 0.587 + b * 0.114).toLong()
                count++
            }
        }

        return if (count > 0) (totalBrightness.toFloat() / (count * 255)).coerceIn(0f, 1f) else 0.5f
    }

    /**
     * Calculate motion between two frames using sampled pixel differencing.
     * Uses stride sampling to avoid allocating full pixel arrays.
     */
    private fun calculateMotion(current: Bitmap, previous: Bitmap): Float {
        if (current.isRecycled || previous.isRecycled) return 0f

        val w = minOf(current.width, previous.width)
        val h = minOf(current.height, previous.height)
        if (w <= 0 || h <= 0) return 0f

        // Use larger stride for motion detection (still accurate enough)
        val stride = maxOf(4, minOf(w, h) / 25)

        var diff = 0f
        var count = 0

        val pixels1 = IntArray(w * h)
        val pixels2 = IntArray(w * h)

        try {
            current.getPixels(pixels1, 0, w, 0, 0, w, h)
            previous.getPixels(pixels2, 0, w, 0, 0, w, h)
        } catch (_: Exception) {
            return 0f
        }

        for (y in 0 until h step stride) {
            for (x in 0 until w step stride) {
                val idx = y * w + x
                val p1 = pixels1[idx]
                val p2 = pixels2[idx]
                val dr = abs(Color.red(p1) - Color.red(p2))
                val dg = abs(Color.green(p1) - Color.green(p2))
                val db = abs(Color.blue(p1) - Color.blue(p2))
                diff += (dr + dg + db) / (3f * 255f)
                count++
            }
        }

        return if (count > 0) (diff / count).coerceIn(0f, 1f) else 0f
    }

    /**
     * Compute color histogram with stride sampling.
     */
    private fun computeHistogram(bitmap: Bitmap): FloatArray {
        val bins = FloatArray(64)
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return bins

        val stride = maxOf(1, minOf(bitmap.width, bitmap.height) / 30)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)

        try {
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        } catch (_: Exception) {
            return bins
        }

        var totalPixels = 0
        for (y in 0 until h step stride) {
            for (x in 0 until w step stride) {
                val pixel = pixels[y * w + x]
                val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114).toInt()
                val bin = (gray * 63 / 255).coerceIn(0, 63)
                bins[bin]++
                totalPixels++
            }
        }

        // Normalize
        val total = totalPixels.toFloat().coerceAtLeast(1f)
        for (i in bins.indices) bins[i] /= total
        return bins
    }

    private fun histogramDifference(h1: FloatArray, h2: FloatArray): Float {
        var diff = 0f
        for (i in h1.indices) {
            diff += abs(h1[i] - h2[i])
        }
        return diff / h1.size
    }

    private fun classifyScene(brightness: Float, motion: Float): SceneType {
        return when {
            motion > 0.4f && brightness > 0.3f -> SceneType.ACTION
            motion < 0.05f && brightness > 0.2f -> SceneType.SPEECH
            motion < 0.02f -> SceneType.STATIC
            motion > 0.5f -> SceneType.TRANSITION
            else -> SceneType.UNKNOWN
        }
    }

    private fun calculateEngagementScore(brightness: Float, motion: Float): Float {
        val brightnessScore = 1f - abs(brightness - 0.5f) * 2f
        val motionScore = when {
            motion in 0.1f..0.5f -> motion * 2f
            motion > 0.5f -> 1f - (motion - 0.5f)
            else -> motion * 3f
        }
        return (brightnessScore * 0.4f + motionScore * 0.6f).coerceIn(0f, 1f)
    }
}
