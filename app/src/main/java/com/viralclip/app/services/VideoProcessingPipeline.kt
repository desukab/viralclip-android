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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main video processing pipeline that orchestrates all AI analysis stages.
 * Flow: Import → Extract Frames → Analyze Audio → Detect Faces → Score Virality → Generate Captions
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
     */
    suspend fun processVideo(
        videoUri: Uri,
        context: Context,
        maxClips: Int = 8
    ): PipelineResult = withContext(Dispatchers.Default) {
        val result = withTimeoutOrNull(30_000L) { // 30 second timeout for entire pipeline
            _state.value = ProcessingState.Analyzing(0f, "Loading video…")

            // Stage 1: Get video metadata
            val videoInfo = ffmpegProcessor.getVideoInfo(videoUri)
            _state.value = ProcessingState.Analyzing(0.1f, "Analyzing video…")

            // Stage 2: Extract frames at 1fps for analysis
            val frames = ffmpegProcessor.extractFrames(videoUri, intervalMs = 2000L)
            _state.value = ProcessingState.Analyzing(0.2f, "Extracted ${frames.size} frames")

            // Stage 3: Analyze audio
            _state.value = ProcessingState.Transcribing(0.3f)
            val audioInfo = audioProcessor.getAudioInfo(videoUri)
            val audioSegments = audioProcessor.analyzeAudioSegments(videoUri)

            // Stage 4: Detect and track faces
            _state.value = ProcessingState.DetectingFaces(0.4f)
            val faceResult = faceTracker.trackFaces(frames)

            // Stage 5: Analyze frames
            _state.value = ProcessingState.Analyzing(0.5f, "Analyzing frames…")
            val frameAnalyses = frameAnalyzer.analyzeFrames(frames)

            // Stage 6: Score virality and find best clips
            _state.value = ProcessingState.ScoringVirality(0.6f)
            val viralityResult = viralityScorer.analyzeAndScore(
                videoUri, videoInfo.durationMs, audioSegments, frames
            )

            // Stage 7: Generate captions for top clips
            _state.value = ProcessingState.GeneratingClips(0.8f)
            val transcription = captionGenerator.generateCaptions(videoUri)

            // Stage 8: Assemble final clips
            _state.value = ProcessingState.GeneratingClips(0.9f)
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
            // Timed out — return minimal result
            _state.value = ProcessingState.Error("Processing timed out. Try a shorter video.")
            PipelineResult(
                videoInfo = FFmpegProcessor.VideoInfo(0, 0, 0, 0, 30f, true, 0),
                audioInfo = AudioProcessor.AudioInfo(44100, 1, 128000, 0, "aac"),
                viralityResult = ViralityScorer.ScoringResult(
                    clips = emptyList(),
                    overallVideoScore = 0f,
                    analysisSummary = "Processing timed out."
                ),
                transcription = CaptionGenerator.TranscriptionResult(
                    segments = emptyList(),
                    language = "en",
                    totalWords = 0,
                    durationMs = 0L
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
                caption.startTimeMs >= scoredClip.startTimeMs &&
                caption.endTimeMs <= scoredClip.endTimeMs
            }.map { caption ->
                caption.copy(
                    startTimeMs = caption.startTimeMs - scoredClip.startTimeMs,
                    endTimeMs = caption.endTimeMs - scoredClip.startTimeMs
                )
            }

            // Get face reframe point for this time range
            val faceFrame = faceResult.frames.find {
                it.timestampMs in scoredClip.startTimeMs..scoredClip.endTimeMs
            }

            Clip(
                projectId = 0, // Will be set when saved
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
            caption.startTimeMs >= clip.startTimeMs &&
            caption.endTimeMs <= clip.endTimeMs
        }.map { caption ->
            caption.copy(
                startTimeMs = caption.startTimeMs - clip.startTimeMs,
                endTimeMs = caption.endTimeMs - clip.startTimeMs
            )
        }

        clip.copy(captions = clipCaptions)
    }

    fun reset() {
        _state.value = ProcessingState.Idle
    }
}
