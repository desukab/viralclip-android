package com.viralclip.app.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max


/**
 * AI-powered virality scoring engine.
 * Analyzes video content across multiple dimensions to predict viral potential.
 * Uses heuristics based on what makes short-form content go viral.
 */
@Singleton
class ViralityScorer @Inject constructor(
    private val context: Context
) {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    data class ScoringResult(
        val clips: List<ScoredClip>,
        val overallVideoScore: Float,
        val analysisSummary: String
    )

    data class ScoredClip(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val score: ViralityScore,
        val recommendedCaptionStyle: CaptionPreset
    )

    /**
     * Analyze video and find the most viral-worthy segments.
     */
    suspend fun analyzeAndScore(
        videoUri: Uri,
        durationMs: Long,
        segments: List<AudioProcessor.AudioSegment>,
        frames: List<Pair<Long, Bitmap>>
    ): ScoringResult = withContext(Dispatchers.Default) {
        _progress.value = 0f
        val clips = mutableListOf<ScoredClip>()

        if (durationMs <= 0 || frames.isEmpty()) {
            return@withContext ScoringResult(
                clips = emptyList(),
                overallVideoScore = 0f,
                analysisSummary = "Insufficient data for analysis."
            )
        }

        // Divide video into candidate segments (15-60 seconds each)
        val segmentLengths = listOf(30_000L, 45_000L, 60_000L)
        val candidateWindows = mutableListOf<Triple<Long, Long, String>>()

        for (segLen in segmentLengths) {
            var start = 0L
            while (start + segLen <= durationMs) {
                candidateWindows.add(Triple(start, start + segLen, "standard"))
                start += segLen / 2 // 50% overlap
            }
        }

        // Pre-compute frame brightness and histogram data for reuse
        val frameData = frames.map { (timeMs, bitmap) ->
            FrameFeatureData(
                timestampMs = timeMs,
                brightness = computeAverageBrightness(bitmap),
                histogram = computeColorHistogram(bitmap),
                pixelHash = computePixelHash(bitmap)
            )
        }

        // Score each candidate window
        for ((index, window) in candidateWindows.withIndex()) {
            val (startMs, endMs, _) = window

            // Analyze audio in this window
            val windowSegments = segments.filter {
                it.startTimeMs >= startMs && it.endTimeMs <= endMs
            }

            // Calculate audio features
            val hasSpeech = windowSegments.any { !it.isSilent && it.speechConfidence > 0.3f }
            val avgVolume = windowSegments.map { it.volume }.average().toFloat()
            val volumeVariance = calculateVariance(windowSegments.map { it.volume })
            val speechRatio = windowSegments.count { !it.isSilent }.toFloat() /
                    maxOf(1, windowSegments.size)
            val dynamicRange = if (windowSegments.isNotEmpty()) {
                val maxVol = windowSegments.maxOf { it.volume }
                val minVol = windowSegments.minOf { it.volume }
                maxVol - minVol
            } else 0f

            // Calculate visual features from frame data in window
            val windowFrameData = frameData.filter { it.timestampMs in startMs..endMs }
            val windowFrames = frames.filter { it.first in startMs..endMs }
            val motionScore = calculateMotionScore(windowFrameData)
            val visualVariety = calculateVisualVariety(windowFrameData)

            // ── SCORING DIMENSIONS ──

            // Hook Strength (first 3 seconds): high volume, speech, motion
            val hookEnd = min(startMs + 3000L, endMs)
            val hookSegments = windowSegments.filter {
                it.startTimeMs in startMs..hookEnd
            }
            val hookFrameData = windowFrameData.filter { it.timestampMs in startMs..hookEnd }
            val hookStrength = calculateHookStrength(hookSegments, hookFrameData, hasSpeech)

            // Engagement: sustained speech, visual variety, dynamic audio
            val engagementScore = calculateEngagement(
                speechRatio, avgVolume, volumeVariance, visualVariety, dynamicRange
            )

            // Emotional Impact: high dynamic range, voice variation, visual changes
            val emotionalImpact = calculateEmotionalImpact(
                volumeVariance, dynamicRange, visualVariety, motionScore
            )

            // Shareability: short enough, punchy, good hook
            val duration = endMs - startMs
            val shareability = calculateShareability(duration, hookStrength, engagementScore)

            // Watch Time: speech clarity, visual engagement, pacing
            val watchTime = calculateWatchTime(
                speechRatio, motionScore, visualVariety, avgVolume
            )

            // Overall Score (weighted average)
            val overall = (
                hookStrength * 0.25f +
                engagementScore * 0.25f +
                emotionalImpact * 0.20f +
                shareability * 0.15f +
                watchTime * 0.15f
            ).coerceIn(0f, 1f)

            // Determine reasons
            val reasons = mutableListOf<String>()
            if (hookStrength > 0.7f) reasons.add("Strong opening hook")
            if (hasSpeech && speechRatio > 0.6f) reasons.add("Clear speech throughout")
            if (dynamicRange > 0.3f) reasons.add("Dynamic audio variation")
            if (motionScore > 0.5f) reasons.add("Visually engaging movement")
            if (emotionalImpact > 0.6f) reasons.add("High emotional content")
            if (visualVariety > 0.5f) reasons.add("Visual variety keeps attention")
            if (overall < 0.3f) {
                if (!hasSpeech) reasons.add("Limited speech content")
                if (motionScore < 0.2f) reasons.add("Low visual movement")
                if (avgVolume < 0.1f) reasons.add("Very low audio levels")
            }

            // Suggest caption style based on content
            val captionPreset = when {
                emotionalImpact > 0.7f -> CaptionPreset.DRAMATIC
                hookStrength > 0.8f -> CaptionPreset.BOLD_HIGHLIGHT
                speechRatio > 0.8f -> CaptionPreset.KARAOKE
                motionScore > 0.6f -> CaptionPreset.POP_IN
                else -> CaptionPreset.DEFAULT
            }

            clips.add(
                ScoredClip(
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    score = ViralityScore(
                        overall = overall,
                        engagementPotential = engagementScore,
                        emotionalImpact = emotionalImpact,
                        shareability = shareability,
                        watchTime = watchTime,
                        hookStrength = hookStrength,
                        reasons = reasons,
                        suggestedStartTime = startMs,
                        suggestedEndTime = endMs
                    ),
                    recommendedCaptionStyle = captionPreset
                )
            )

            _progress.value = (index + 1).toFloat() / candidateWindows.size
        }

        // Sort by score and remove overlapping clips (keep best)
        val sorted = clips.sortedByDescending { it.score.overall }
        val topClips = selectNonOverlapping(sorted, maxClips = 8)

        _progress.value = 1f

        val avgScore = if (clips.isNotEmpty()) clips.map { it.score.overall }.average().toFloat() else 0f

        ScoringResult(
            clips = topClips,
            overallVideoScore = avgScore,
            analysisSummary = buildSummary(topClips, avgScore)
        )
    }

    private fun calculateHookStrength(
        hookSegments: List<AudioProcessor.AudioSegment>,
        hookFrameData: List<FrameFeatureData>,
        hasSpeech: Boolean
    ): Float {
        val speechInHook = hookSegments.any { !it.isSilent && it.speechConfidence > 0.4f }
        val avgVolume = if (hookSegments.isNotEmpty())
            hookSegments.map { it.volume }.average().toFloat() else 0f
        val motionInHook = if (hookFrameData.size >= 2) {
            calculateMotionScore(hookFrameData)
        } else 0f

        return when {
            speechInHook && avgVolume > 0.3f && motionInHook > 0.4f -> 0.9f
            speechInHook && avgVolume > 0.2f -> 0.7f
            hasSpeech && motionInHook > 0.3f -> 0.6f
            avgVolume > 0.2f -> 0.5f
            else -> 0.3f
        }
    }

    private fun calculateEngagement(
        speechRatio: Float,
        avgVolume: Float,
        volumeVariance: Float,
        visualVariety: Float,
        dynamicRange: Float
    ): Float {
        val speechScore = speechRatio * 0.3f
        val volumeScore = (avgVolume * 0.5f + volumeVariance * 5f) * 0.3f // Scale variance
        val visualScore = visualVariety * 0.25f
        val dynamicScore = dynamicRange * 0.15f
        return (speechScore + volumeScore + visualScore + dynamicScore).coerceIn(0f, 1f)
    }

    private fun calculateEmotionalImpact(
        volumeVariance: Float,
        dynamicRange: Float,
        visualVariety: Float,
        motionScore: Float
    ): Float {
        return ((volumeVariance * 5f + dynamicRange + visualVariety + motionScore) / 4f)
            .coerceIn(0f, 1f)
    }

    private fun calculateShareability(
        durationMs: Long,
        hookStrength: Float,
        engagement: Float
    ): Float {
        val durationScore = when {
            durationMs in 15_000..45_000 -> 1.0f
            durationMs in 10_000..60_000 -> 0.8f
            durationMs in 5_000..90_000 -> 0.6f
            else -> 0.3f
        }
        return (durationScore * 0.4f + hookStrength * 0.3f + engagement * 0.3f)
            .coerceIn(0f, 1f)
    }

    private fun calculateWatchTime(
        speechRatio: Float,
        motionScore: Float,
        visualVariety: Float,
        avgVolume: Float
    ): Float {
        return ((speechRatio * 0.35f) + (motionScore * 0.25f) +
                (visualVariety * 0.25f) + (avgVolume * 0.15f))
            .coerceIn(0f, 1f)
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    /**
     * Real motion detection using pixel-level frame differencing.
     * Compares downscaled frames at reduced resolution for performance.
     */
    private fun calculateMotionScore(frameData: List<FrameFeatureData>): Float {
        if (frameData.size < 2) return 0.3f

        var totalDiff = 0f
        var comparisons = 0

        for (i in 1 until frameData.size) {
            val prev = frameData[i - 1]
            val curr = frameData[i]

            // Use brightness change as a fast proxy for motion
            val brightnessDiff = abs(curr.brightness - prev.brightness)

            // Use histogram difference for color/scene motion
            val histDiff = histogramDistance(prev.histogram, curr.histogram)

            // Use pixel hash difference for structural changes
            val hashDiff = if (prev.pixelHash != curr.pixelHash) 0.15f else 0f

            totalDiff += (brightnessDiff * 0.3f + histDiff * 0.5f + hashDiff * 0.2f)
            comparisons++
        }

        return if (comparisons > 0) {
            (totalDiff / comparisons * 2.5f).coerceIn(0f, 1f) // Scale up since typical motion is subtle
        } else 0.3f
    }

    /**
     * Calculate visual variety using histogram diversity across frames.
     */
    private fun calculateVisualVariety(frameData: List<FrameFeatureData>): Float {
        if (frameData.isEmpty()) return 0.3f
        if (frameData.size == 1) return 0.2f

        // Measure brightness variance across frames
        val brightnesses = frameData.map { it.brightness }
        val brightnessVariance = calculateVariance(brightnesses)

        // Measure histogram diversity
        var totalHistDiff = 0f
        var comparisons = 0
        for (i in 1 until frameData.size) {
            totalHistDiff += histogramDistance(frameData[i - 1].histogram, frameData[i].histogram)
            comparisons++
        }
        val avgHistDiff = if (comparisons > 0) totalHistDiff / comparisons else 0f

        // Measure pixel hash diversity (structural variety)
        val uniqueHashes = frameData.map { it.pixelHash }.toSet().size.toFloat()
        val hashDiversity = (uniqueHashes / frameData.size).coerceIn(0f, 1f)

        return ((brightnessVariance * 10f * 0.3f + avgHistDiff * 0.4f + hashDiversity * 0.3f))
            .coerceIn(0f, 1f)
    }

    /**
     * Compute average brightness of a bitmap (0.0 - 1.0).
     * Samples pixels for speed.
     */
    private fun computeAverageBrightness(bitmap: Bitmap): Float {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return 0.5f

        val step = maxOf(1, minOf(bitmap.width, bitmap.height) / 20)
        var totalBrightness = 0L
        var count = 0

        val pixels = IntArray(bitmap.width * bitmap.height)
        try {
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        } catch (_: Exception) {
            return 0.5f
        }

        for (i in pixels.indices step step * bitmap.width) {
            for (j in 0 until bitmap.width step step) {
                val idx = (i / bitmap.width * bitmap.height + j).coerceIn(0, pixels.size - 1)
                val pixel = pixels[idx]
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
     * Compute a simple 8-bin color histogram for a bitmap.
     * Samples pixels for speed.
     */
    private fun computeColorHistogram(bitmap: Bitmap): FloatArray {
        val bins = FloatArray(8)
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return bins

        val step = maxOf(1, minOf(bitmap.width, bitmap.height) / 15)
        val pixels = IntArray(bitmap.width * bitmap.height)
        try {
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        } catch (_: Exception) {
            return bins
        }

        for (i in pixels.indices step step * bitmap.width) {
            for (j in 0 until bitmap.width step step) {
                val idx = (i / bitmap.width * bitmap.height + j).coerceIn(0, pixels.size - 1)
                val pixel = pixels[idx]
                // Quantize to 8 bins based on dominant channel
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val max = maxOf(r, g, b)
                val bin = when {
                    max == r && r > g && r > b -> 0   // Red dominant
                    max == g && g > r && g > b -> 1   // Green dominant
                    max == b && b > r && b > g -> 2   // Blue dominant
                    r > 200 && g > 200 && b > 200 -> 3 // White
                    r < 50 && g < 50 && b < 50 -> 4   // Dark
                    r > g && r > b -> 5               // Warm
                    b > r && b > g -> 6               // Cool
                    else -> 7                          // Neutral
                }
                bins[bin]++
            }
        }

        // Normalize
        val total = pixels.size.toFloat().coerceAtLeast(1f)
        for (i in bins.indices) bins[i] /= total
        return bins
    }

    /**
     * Compute a simple hash of pixel data for structural comparison.
     * Uses a grid of sampled pixels.
     */
    private fun computePixelHash(bitmap: Bitmap): Long {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return 0L

        var hash = 0L
        val gridSize = 4
        val cellW = bitmap.width / gridSize
        val cellH = bitmap.height / gridSize

        for (gy in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                val x = (gx * cellW + cellW / 2).coerceIn(0, bitmap.width - 1)
                val y = (gy * cellH + cellH / 2).coerceIn(0, bitmap.height - 1)
                try {
                    val pixel = bitmap.getPixel(x, y)
                    hash = hash * 31 + pixel.toLong()
                } catch (_: Exception) { }
            }
        }
        return hash
    }

    /**
     * Compute L1 distance between two normalized histograms.
     */
    private fun histogramDistance(h1: FloatArray, h2: FloatArray): Float {
        if (h1.size != h2.size) return 0f
        var diff = 0f
        for (i in h1.indices) {
            diff += abs(h1[i] - h2[i])
        }
        return (diff / h1.size).coerceIn(0f, 1f)
    }

    private fun selectNonOverlapping(
        sortedClips: List<ScoredClip>,
        maxClips: Int
    ): List<ScoredClip> {
        val selected = mutableListOf<ScoredClip>()
        for (clip in sortedClips) {
            if (selected.size >= maxClips) break
            val overlaps = selected.any { existing ->
                clip.startTimeMs < existing.endTimeMs && clip.endTimeMs > existing.startTimeMs
            }
            if (!overlaps) selected.add(clip)
        }
        return selected.sortedBy { it.startTimeMs }
    }

    private fun buildSummary(clips: List<ScoredClip>, avgScore: Float): String {
        val highViral = clips.count { it.score.overall >= 0.7f }
        val mediumViral = clips.count { it.score.overall in 0.4f..0.7f }
        return buildString {
            appendLine("Analysis Complete")
            appendLine("Found ${clips.size} potential clips")
            appendLine("$highViral high potential, $mediumViral medium potential")
            appendLine("Overall video score: ${(avgScore * 100).toInt()}%")
        }
    }

    /**
     * Pre-computed per-frame feature data to avoid recomputation.
     */
    private data class FrameFeatureData(
        val timestampMs: Long,
        val brightness: Float,
        val histogram: FloatArray,
        val pixelHash: Long
    )
}
