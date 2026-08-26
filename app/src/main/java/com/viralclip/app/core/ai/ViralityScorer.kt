package com.viralclip.app.core.ai

import android.content.Context
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.core.analysis.FrameAnalyzer
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
        frames: List<Pair<Long, android.graphics.Bitmap>>
    ): ScoringResult = withContext(Dispatchers.Default) {
        _progress.value = 0f
        val clips = mutableListOf<ScoredClip>()

        // Divide video into candidate segments (15-60 seconds each)
        val segmentLengths = listOf(30_000L, 45_000L, 60_000L) // Try different lengths
        val candidateWindows = mutableListOf<Triple<Long, Long, String>>()

        for (segLen in segmentLengths) {
            var start = 0L
            while (start + segLen <= durationMs) {
                candidateWindows.add(Triple(start, start + segLen, "standard"))
                start += segLen / 2 // 50% overlap
            }
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

            // Calculate visual features from frames in window
            val windowFrames = frames.filter { it.first in startMs..endMs }
            val motionScore = calculateMotionScore(windowFrames)
            val visualVariety = calculateVisualVariety(windowFrames)
            val hasFace = windowFrames.any { it.second.width > 0 } // Simplified

            // ── SCORING DIMENSIONS ──

            // Hook Strength (first 3 seconds): high volume, speech, motion
            val hookStart = startMs
            val hookEnd = min(startMs + 3000L, endMs)
            val hookSegments = windowSegments.filter {
                it.startTimeMs in hookStart..hookEnd
            }
            val hookStrength = calculateHookStrength(hookSegments, motionScore, hasSpeech)

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
        motionScore: Float,
        hasSpeech: Boolean
    ): Float {
        val speechInHook = hookSegments.any { !it.isSilent && it.speechConfidence > 0.4f }
        val avgVolume = hookSegments.map { it.volume }.average().toFloat()
        return when {
            speechInHook && avgVolume > 0.3f && motionScore > 0.4f -> 0.9f
            speechInHook && avgVolume > 0.2f -> 0.7f
            hasSpeech && motionScore > 0.3f -> 0.6f
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
        val volumeScore = (avgVolume * 0.5f + volumeVariance * 0.5f) * 0.3f
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
        return ((volumeVariance + dynamicRange + visualVariety + motionScore) / 4f)
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

    private fun calculateMotionScore(frames: List<Pair<Long, android.graphics.Bitmap>>): Float {
        if (frames.size < 2) return 0.3f
        // Simplified motion detection - compare consecutive frames
        var totalDiff = 0f
        for (i in 1 until frames.size) {
            val prev = frames[i - 1].second
            val curr = frames[i].second
            // Simple pixel difference ratio
            val size = min(prev.width * prev.height, curr.width * curr.height)
            if (size > 0) {
                totalDiff += 0.1f // Placeholder motion score
            }
        }
        return (totalDiff / (frames.size - 1)).coerceIn(0f, 1f)
    }

    private fun calculateVisualVariety(frames: List<Pair<Long, android.graphics.Bitmap>>): Float {
        if (frames.isEmpty()) return 0.3f
        // Simplified: more frames with different content = higher variety
        return (frames.size.coerceAtMost(10) / 10f).coerceIn(0f, 1f)
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
}
