package com.viralclip.app.services

import android.content.Context
import android.net.Uri
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.core.ai.ViralityScorer
import com.viralclip.app.core.ai.CaptionGenerator
import com.viralclip.app.core.ai.FaceTracker
import com.viralclip.app.core.analysis.FrameAnalyzer
import com.viralclip.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main video processing pipeline that orchestrates all AI analysis stages.
 * Flow: Import → Extract Frames → Analyze Audio → Detect Faces → Score Virality → Generate Captions
 *
 * Uses adaptive timeout based on video duration.
 */
@Singleton
class VideoProcessingPipeline @Inject constructor(
    private val ffmpegProcessor: FFmpegProcessor,
    private val audioProcessor: AudioProcessor,
    private val viralityScorer: ViralityScorer,
    private val captionGenerator: CaptionGenerator,
    private val faceTracker: FaceTracker,
    private val frameAnalyzer: FrameAnalyzer
) {
    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state

    data class PipelineResult(
        val videoInfo: FFmpegProcessor.VideoInfo,
        val audioInfo: AudioProcessor.AudioInfo,
        val viralityResult: ViralityScorer.ScoringResult,
        val transcription: CaptionGenerator.TranscriptionResult,
        val faceTrackResult: FaceTracker.FaceTrackResult,
        val frameAnalyses: List<FrameAnalysis>,
        val generatedClips: List<Clip>
    )

    /**
     * Run the full processing pipeline on a video.
     * Timeout scales with video duration: base 60s + 2s per 10s of video.
     */
    suspend fun processVideo(
        videoUri: Uri,
        context: Context,
        maxClips: Int = 8
    ): PipelineResult = withContext(Dispatchers.Default) {
        // Stage 1: Get video metadata (fast, no timeout needed)
        _state.value = ProcessingState.Analyzing(0f, "Loading video…")
        val videoInfo = ffmpegProcessor.getVideoInfo(videoUri)

        // Adaptive timeout: 60s base + 2s per 10s of video, capped at 5 minutes
        val timeoutMs = (60_000L + (videoInfo.durationMs / 10_000L) * 2_000L)
            .coerceIn(60_000L, 300_000L)

        val result = withTimeoutOrNull(timeoutMs) {
            _state.value = ProcessingState.Analyzing(0.05f, "Analyzing video…")

            // Stage 2: Extract frames at 2s intervals, downscaled to 360px
            val frames = ffmpegProcessor.extractFrames(
                videoUri, intervalMs = 2000L, maxFrames = 30, targetWidth = 360
            )
            _state.value = ProcessingState.Analyzing(0.15f, "Extracted ${frames.size} frames")

            // Stage 3: Analyze audio
            _state.value = ProcessingState.Transcribing(0.25f)
            val audioInfo = audioProcessor.getAudioInfo(videoUri)
            val audioSegments = audioProcessor.analyzeAudioSegments(videoUri)

            // Stage 4: Detect and track faces
            _state.value = ProcessingState.DetectingFaces(0.40f)
            val faceResult = faceTracker.trackFaces(frames)

            // Stage 5: Analyze frames
            _state.value = ProcessingState.Analyzing(0.55f, "Analyzing frames…")
            val frameAnalyses = frameAnalyzer.analyzeFrames(frames)

            // Stage 6: Score virality and find best clips
            _state.value = ProcessingState.ScoringVirality(0.65f)
            val viralityResult = viralityScorer.analyzeAndScore(
                videoUri, videoInfo.durationMs, audioSegments, frames
            )

            // Stage 7: Generate captions for top clips
            _state.value = ProcessingState.GeneratingClips(0.80f)
            val transcription = captionGenerator.generateCaptions(videoUri)

            // Stage 8: Assemble final clips
            _state.value = ProcessingState.GeneratingClips(0.90f)
            val generatedClips = assembleClips(
                videoUri, viralityResult, transcription, faceResult
            )

            PipelineResult(
                videoInfo = videoInfo,
                audioInfo = audioInfo,
                viralityResult = viralityResult,
                transcription = transcription,
                faceTrackResult = faceResult,
                frameAnalyses = frameAnalyses,
                generatedClips = generatedClips
            )
        }

        if (result != null) {
            _state.value = ProcessingState.Complete
            result
        } else {
            _state.value = ProcessingState.Error(
                "Processing timed out. Try a shorter video or lower resolution."
            )
            PipelineResult(
                videoInfo = videoInfo,
                audioInfo = AudioProcessor.AudioInfo(44100, 1, 128000, videoInfo.durationMs, "aac"),
                viralityResult = ViralityScorer.ScoringResult(
                    clips = emptyList(),
                    overallVideoScore = 0f,
                    analysisSummary = "Processing timed out."
                ),
                transcription = CaptionGenerator.TranscriptionResult(
                    segments = emptyList(),
                    language = "en",
                    totalWords = 0,
                    durationMs = videoInfo.durationMs
                ),
                faceTrackResult = FaceTracker.FaceTrackResult(
                    frames = emptyList(),
                    dominantSpeaker = null,
                    avgFaceSize = 0f,
                    facePresentRatio = 0f
                ),
                frameAnalyses = emptyList(),
                generatedClips = emptyList()
            )
        }
    }

    /**
     * Assemble clips from scoring results with captions applied.
     */
    private fun assembleClips(
        videoUri: Uri,
        scoringResult: ViralityScorer.ScoringResult,
        transcription: CaptionGenerator.TranscriptionResult,
        faceResult: FaceTracker.FaceTrackResult
    ): List<Clip> {
        return scoringResult.clips.mapIndexed { index, scoredClip ->
            // Filter captions for this clip's time range
            val clipCaptions = transcription.segments.filter { caption ->
                caption.startTimeMs <= scoredClip.endTimeMs &&
                caption.endTimeMs >= scoredClip.startTimeMs
            }.map { caption ->
                val adjustedStart = maxOf(0L, caption.startTimeMs - scoredClip.startTimeMs)
                val adjustedEnd = minOf(
                    scoredClip.endTimeMs - scoredClip.startTimeMs,
                    caption.endTimeMs - scoredClip.startTimeMs
                )
                caption.copy(
                    startTimeMs = adjustedStart,
                    endTimeMs = adjustedEnd
                )
            }.filter { it.endTimeMs > it.startTimeMs }

            // Get face reframe point for this time range
            val faceFrame = faceResult.frames.find {
                it.timestampMs in scoredClip.startTimeMs..scoredClip.endTimeMs
            }

            Clip(
                projectId = 0,
                name = "Clip ${index + 1}",
                sourceVideoUri = videoUri.toString(),
                startTimeMs = scoredClip.startTimeMs,
                endTimeMs = scoredClip.endTimeMs,
                order = index,
                viralityScore = scoredClip.score.overall,
                captions = clipCaptions,
                captionStyle = CaptionStyle(
                    preset = scoredClip.recommendedCaptionStyle
                )
            )
        }
    }

    /**
     * Re-process a single clip's captions.
     */
    suspend fun regenerateCaptions(
        clip: Clip,
        language: String = "en"
    ): Clip = withContext(Dispatchers.Default) {
        val videoUri = Uri.parse(clip.sourceVideoUri)
        val transcription = captionGenerator.generateCaptions(videoUri, language)

        val clipCaptions = transcription.segments.filter { caption ->
            caption.startTimeMs <= clip.endTimeMs &&
            caption.endTimeMs >= clip.startTimeMs
        }.map { caption ->
            val adjustedStart = maxOf(0L, caption.startTimeMs - clip.startTimeMs)
            val adjustedEnd = minOf(
                clip.endTimeMs - clip.startTimeMs,
                caption.endTimeMs - clip.startTimeMs
            )
            caption.copy(
                startTimeMs = adjustedStart,
                endTimeMs = adjustedEnd
            )
        }.filter { it.endTimeMs > it.startTimeMs }

        clip.copy(captions = clipCaptions)
    }

    fun reset() {
        _state.value = ProcessingState.Idle
    }
}
