package com.viralclip.app.core.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio processing engine for extraction, analysis, volume detection,
 * and speech confidence estimation.
 */
@Singleton
class AudioProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    data class AudioInfo(
        val sampleRate: Int,
        val channels: Int,
        val bitrate: Int,
        val durationMs: Long,
        val format: String
    )

    data class AudioSegment(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val volume: Float,       // 0.0 - 1.0 average volume
        val isSilent: Boolean,
        val speechConfidence: Float
    )

    /**
     * Extract audio information from a video file.
     * Returns default values on failure.
     */
    suspend fun getAudioInfo(videoUri: Uri): AudioInfo = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, videoUri, null)

            var sampleRate = 44100
            var channels = 1
            var bitrate = 128000
            var duration = 0L
            var format = "aac"

            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""

                if (mime.startsWith("audio/")) {
                    sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    bitrate = if (trackFormat.containsKey(MediaFormat.KEY_BIT_RATE))
                        trackFormat.getInteger(MediaFormat.KEY_BIT_RATE) else 128000
                    duration = if (trackFormat.containsKey(MediaFormat.KEY_DURATION))
                        trackFormat.getLong(MediaFormat.KEY_DURATION) / 1000 else 0L
                    format = mime.removePrefix("audio/")
                    break
                }
            }

            AudioInfo(sampleRate, channels, bitrate, duration, format)
        } catch (e: Exception) {
            AudioInfo(44100, 1, 128000, 0, "aac")
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Extract audio track from video as separate file.
     * Uses try-finally to guarantee resource cleanup.
     */
    suspend fun extractAudio(
        videoUri: Uri,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            onProgress(0f)
            extractor = MediaExtractor()
            extractor.setDataSource(context, videoUri, null)

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) return@withContext false

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val trackIndex = muxer.addTrack(format)
            muxer.start()

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val duration = if (format.containsKey(MediaFormat.KEY_DURATION))
                format.getLong(MediaFormat.KEY_DURATION) else 1L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                extractor.advance()

                if (bufferInfo.presentationTimeUs > 0 && duration > 0) {
                    onProgress((bufferInfo.presentationTimeUs.toFloat() / duration).coerceIn(0f, 0.99f))
                }
            }

            onProgress(1f)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    /**
     * Analyze audio levels in segments for silence detection and speech confidence.
     * Reads raw audio samples and computes RMS amplitude per segment.
     */
    suspend fun analyzeAudioSegments(
        videoUri: Uri,
        segmentDurationMs: Long = 1000L,
        maxSegments: Int = 300
    ): List<AudioSegment> = withContext(Dispatchers.IO) {
        val segments = mutableListOf<AudioSegment>()
        val startTime = System.currentTimeMillis()
        var extractor: MediaExtractor? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, videoUri, null)

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) return@withContext segments

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            val buffer = ByteBuffer.allocate(8192)
            var segmentAmplitudeSum = 0.0
            var segmentSampleCount = 0
            var currentSegmentStart = 0L
            val segmentDurationUs = segmentDurationMs * 1000

            while (segments.size < maxSegments) {
                // Timeout after 10 seconds
                if (System.currentTimeMillis() - startTime > 10_000) break

                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break

                val timeUs = extractor.sampleTime

                // Compute RMS amplitude from PCM samples in the buffer
                try {
                    buffer.position(0)
                    var sumSquares = 0.0
                    var count = 0
                    while (buffer.hasRemaining() && buffer.remaining() >= 2) {
                        val sample = buffer.short.toFloat() / Short.MAX_VALUE
                        sumSquares += sample * sample
                        count++
                    }
                    if (count > 0) {
                        segmentAmplitudeSum += sumSquares / count
                        segmentSampleCount++
                    }
                } catch (_: Exception) {
                    // Buffer read error, skip this chunk
                }

                // Check if we've crossed a segment boundary
                if (timeUs - currentSegmentStart >= segmentDurationUs) {
                    val rmsAmplitude = if (segmentSampleCount > 0)
                        Math.sqrt(segmentAmplitudeSum / segmentSampleCount).toFloat() else 0f

                    // Clamp RMS to 0-1 range (typical speech RMS is 0.01-0.3)
                    val normalizedVolume = (rmsAmplitude * 3f).coerceIn(0f, 1f)

                    // Speech confidence: higher RMS = more likely speech
                    // Silence threshold ~0.01, typical speech ~0.05-0.2
                    val isSilent = normalizedVolume < 0.02f
                    val speechConfidence = when {
                        normalizedVolume < 0.02f -> 0f        // Silence
                        normalizedVolume < 0.05f -> 0.3f      // Whisper / background
                        normalizedVolume < 0.15f -> 0.7f      // Normal speech
                        normalizedVolume < 0.3f -> 0.9f       // Loud speech
                        else -> 1.0f                          // Very loud / shouting
                    }

                    segments.add(
                        AudioSegment(
                            startTimeMs = currentSegmentStart / 1000,
                            endTimeMs = timeUs / 1000,
                            volume = normalizedVolume,
                            isSilent = isSilent,
                            speechConfidence = speechConfidence
                        )
                    )
                    currentSegmentStart = timeUs
                    segmentAmplitudeSum = 0.0
                    segmentSampleCount = 0
                }

                buffer.clear()
                extractor.advance()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
        }
        segments
    }

    /**
     * Get peak audio level for waveform visualization.
     */
    suspend fun getAudioWaveform(
        videoUri: Uri,
        numSamples: Int = 100
    ): List<Float> = withContext(Dispatchers.IO) {
        val waveform = mutableListOf<Float>()
        try {
            val segments = analyzeAudioSegments(videoUri, 100L)
            if (segments.isEmpty()) return@withContext List(numSamples) { 0f }

            val samplesPerWaveformPoint = maxOf(1, segments.size / numSamples)

            for (i in 0 until numSamples) {
                val startIdx = i * samplesPerWaveformPoint
                val endIdx = minOf(startIdx + samplesPerWaveformPoint, segments.size)
                if (startIdx < segments.size) {
                    val avgVolume = segments.subList(startIdx, endIdx).map { it.volume }.average()
                    waveform.add(avgVolume.toFloat())
                } else {
                    waveform.add(0f)
                }
            }
        } catch (e: Exception) {
            repeat(numSamples) { waveform.add(0f) }
        }
        waveform
    }

    /**
     * Calculate audio energy distribution for finding peaks and valleys.
     */
    suspend fun getAudioEnergyPeaks(
        videoUri: Uri,
        threshold: Float = 0.3f
    ): List<Pair<Long, Float>> = withContext(Dispatchers.IO) {
        val peaks = mutableListOf<Pair<Long, Float>>()
        try {
            val segments = analyzeAudioSegments(videoUri, 200L)
            var prevVolume = 0f
            for ((index, segment) in segments.withIndex()) {
                if (index > 0 && segment.volume > prevVolume && segment.volume > threshold) {
                    peaks.add(segment.startTimeMs to segment.volume)
                }
                prevVolume = segment.volume
            }
        } catch (_: Exception) {}
        peaks
    }
}
