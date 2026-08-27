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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

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
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    data class PipelineResult(
        val videoInfo: FFmpegProcessor.VideoInfo,
        val audioInfo: AudioProcessor.AudioInfo,
        val viralityResult: ViralityScorer.ScoringResult,
        val transcription: CaptionGenerator.TranscriptionResult,
        val faceTrackResult: FaceTracker.FaceTrackResult,
        val frameAnalyses: List<FrameAnalysis>,
        val generatedClips: List<Clip>
    )

    private var lastResult: PipelineResult? = null
    val cachedResult: PipelineResult? get() = lastResult

    suspend fun processVideo(
        videoUri: Uri,
        context: Context,
        maxClips: Int = 8
    ): PipelineResult = withContext(Dispatchers.Default) {
        _state.value = ProcessingState.Analyzing(0f, "Loading video…")
        val videoInfo = try {
            ffmpegProcessor.getVideoInfo(videoUri)
        } catch (e: Exception) {
            _state.value = ProcessingState.Error("Could not read video metadata: ${e.message}")
            throw e
        }

        val timeoutMs = (60_000L + (videoInfo.durationMs / 10_000L) * 2_000L)
            .coerceIn(60_000L, 600_000L)

        val result = try {
            withTimeoutOrNull(timeoutMs) {
                runPipelineStages(videoUri, context, maxClips)
            }
        } catch (e: CancellationException) {
            _state.value = ProcessingState.Error("Processing cancelled")
            throw e
        } catch (e: Exception) {
            _state.value = ProcessingState.Error("Processing failed: ${e.message}", e)
            throw e
        }

        val finalResult = result ?: buildEmptyResult(videoInfo)
        lastResult = finalResult
        _state.value = ProcessingState.Complete
        finalResult
    }

    private suspend fun runPipelineStages(
        videoUri: Uri,
        context: Context,
        maxClips: Int
    ): PipelineResult = withContext(Dispatchers.Default) {
        _state.value = ProcessingState.Analyzing(0.05f, "Analyzing video…")

        val frames = ffmpegProcessor.extractFrames(
            videoUri, intervalMs = 2000L, maxFrames = 30, targetWidth = 360
        )
        _state.value = ProcessingState.Analyzing(0.15f, "Extracted ${frames.size} frames")

        _state.value = ProcessingState.Transcribing(0.25f)
        val audioInfo = audioProcessor.getAudioInfo(videoUri)
        val audioSegments = audioProcessor.analyzeAudioSegments(videoUri)

        _state.value = ProcessingState.DetectingFaces(0.40f)
        val faceResult = faceTracker.trackFaces(frames)

        _state.value = ProcessingState.Analyzing(0.55f, "Analyzing frames…")
        val frameAnalyses = frameAnalyzer.analyzeFrames(frames)

        _state.value = ProcessingState.ScoringVirality(0.65f)
        val viralityResult = viralityScorer.analyzeAndScore(
            videoUri, videoInfoDurationMs(audioInfo), audioSegments, frames
        )

        _state.value = ProcessingState.GeneratingClips(0.80f)
        val transcription = captionGenerator.generateCaptions(videoUri)

        _state.value = ProcessingState.GeneratingClips(0.90f)
        val generatedClips = assembleClips(
            videoUri, viralityResult, transcription, faceResult, maxClips
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

    private fun videoInfoDurationMs(audio: AudioProcessor.AudioInfo): Long = audio.durationMs

    private fun buildEmptyResult(videoInfo: FFmpegProcessor.VideoInfo): PipelineResult = PipelineResult(
        videoInfo = videoInfo,
        audioInfo = AudioProcessor.AudioInfo(44100, 1, 128000, videoInfo.durationMs, "aac"),
        viralityResult = ViralityScorer.ScoringResult(
            clips = emptyList(),
            overallVideoScore = 0f,
            analysisSummary = "Processing timed out. Try a shorter video."
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

    private fun assembleClips(
        videoUri: Uri,
        scoringResult: ViralityScorer.ScoringResult,
        transcription: CaptionGenerator.TranscriptionResult,
        faceResult: FaceTracker.FaceTrackResult,
        maxClips: Int
    ): List<Clip> {
        val baseProjectId = 0L
        return scoringResult.clips
            .sortedByDescending { it.score.overall }
            .take(maxClips)
            .mapIndexed { index, scoredClip ->
                val clipDuration = scoredClip.endTimeMs - scoredClip.startTimeMs

                val clipCaptions = transcription.segments
                    .filter { caption ->
                        caption.startTimeMs <= scoredClip.endTimeMs &&
                            caption.endTimeMs >= scoredClip.startTimeMs
                    }
                    .map { caption ->
                        val adjustedStart = maxOf(0L, caption.startTimeMs - scoredClip.startTimeMs)
                        val adjustedEnd = minOf(clipDuration, caption.endTimeMs - scoredClip.startTimeMs)
                        caption.copy(startTimeMs = adjustedStart, endTimeMs = adjustedEnd)
                    }
                    .filter { it.endTimeMs > it.startTimeMs }

                val faceFrame = faceResult.frames.find { frame ->
                    frame.timestampMs in scoredClip.startTimeMs..scoredClip.endTimeMs
                }

                Clip(
                    projectId = baseProjectId,
                    name = "Clip ${index + 1}",
                    sourceVideoUri = videoUri.toString(),
                    startTimeMs = scoredClip.startTimeMs,
                    endTimeMs = scoredClip.endTimeMs,
                    order = index,
                    viralityScore = scoredClip.score.overall,
                    captions = clipCaptions,
                    captionStyle = CaptionStyle(preset = scoredClip.recommendedCaptionStyle),
                    selected = index == 0
                )
            }
    }

    suspend fun regenerateCaptions(
        clip: Clip,
        language: String = "en"
    ): Clip = withContext(Dispatchers.Default + NonCancellable) {
        val videoUri = Uri.parse(clip.sourceVideoUri)
        _state.value = ProcessingState.Transcribing(0f)
        val transcription = try {
            captionGenerator.generateCaptions(videoUri, language)
        } catch (e: Exception) {
            _state.value = ProcessingState.Error("Caption generation failed: ${e.message}")
            throw e
        }

        val clipCaptions = transcription.segments
            .filter { it.startTimeMs <= clip.endTimeMs && it.endTimeMs >= clip.startTimeMs }
            .map { caption ->
                val adjustedStart = maxOf(0L, caption.startTimeMs - clip.startTimeMs)
                val adjustedEnd = minOf(
                    clip.endTimeMs - clip.startTimeMs,
                    caption.endTimeMs - clip.startTimeMs
                )
                caption.copy(startTimeMs = adjustedStart, endTimeMs = adjustedEnd)
            }
            .filter { it.endTimeMs > it.startTimeMs }

        _state.value = ProcessingState.Complete
        clip.copy(captions = clipCaptions)
    }

    fun reset() {
        _state.value = ProcessingState.Idle
    }

    fun cancel() {
        _state.value = ProcessingState.Idle
    }
}
