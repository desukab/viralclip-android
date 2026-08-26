package com.viralclip.app.core.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
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
 * Audio processing engine for extraction, transcription prep, volume adjustment,
 * and background noise analysis.
 */
@Singleton
class AudioProcessor @Inject constructor(
    private val context: Context
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
     */
    suspend fun getAudioInfo(videoUri: Uri): AudioInfo = withContext(Dispatchers.IO) {
        try {
            val extractor = MediaExtractor()
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
                    bitrate = trackFormat.getInteger(MediaFormat.KEY_BIT_RATE)
                    duration = trackFormat.getLong(MediaFormat.KEY_DURATION) / 1000
                    format = mime.removePrefix("audio/")
                    break
                }
            }

            extractor.release()

            AudioInfo(sampleRate, channels, bitrate, duration, format)
        } catch (e: Exception) {
            AudioInfo(44100, 1, 128000, 0, "aac")
        }
    }

    /**
     * Extract audio track from video as separate file.
     */
    suspend fun extractAudio(
        videoUri: Uri,
        outputFile: File,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(0f)
            val extractor = MediaExtractor()
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
            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val trackIndex = muxer.addTrack(format)
            muxer.start()

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                extractor.advance()

                if (bufferInfo.presentationTimeUs > 0) {
                    val duration = format.getLong(MediaFormat.KEY_DURATION)
                    if (duration > 0) {
                        onProgress(bufferInfo.presentationTimeUs.toFloat() / duration)
                    }
                }
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            onProgress(1f)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Analyze audio levels in segments for silence detection and speech confidence.
     */
    suspend fun analyzeAudioSegments(
        videoUri: Uri,
        segmentDurationMs: Long = 1000L,
        maxSegments: Int = 120
    ): List<AudioSegment> = withContext(Dispatchers.IO) {
        val segments = mutableListOf<AudioSegment>()
        val startTime = System.currentTimeMillis()
        try {
            val extractor = MediaExtractor()
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
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)

            val buffer = ByteBuffer.allocate(4096)
            var totalAmplitude = 0.0
            var sampleCount = 0
            var currentSegmentStart = 0L

            while (segments.size < maxSegments) {
                // Timeout after 5 seconds
                if (System.currentTimeMillis() - startTime > 5_000) break

                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break

                val timeUs = extractor.sampleTime

                // Simple amplitude calculation from PCM samples
                try {
                    buffer.position(0)
                    var amplitude = 0.0
                    var count = 0
                    while (buffer.hasRemaining() && buffer.remaining() >= 2) {
                        val sample = buffer.short.toFloat() / Short.MAX_VALUE
                        amplitude += Math.abs(sample.toDouble())
                        count++
                    }
                    if (count > 0) {
                        totalAmplitude += amplitude / count
                        sampleCount++
                    }
                } catch (_: Exception) {
                    // Buffer read error, skip this sample
                }

                // Check if we've crossed a segment boundary
                if (timeUs - currentSegmentStart >= segmentDurationMs * 1000) {
                    val avgVolume = if (sampleCount > 0) (totalAmplitude / sampleCount).toFloat() else 0f
                    segments.add(
                        AudioSegment(
                            startTimeMs = currentSegmentStart / 1000,
                            endTimeMs = timeUs / 1000,
                            volume = avgVolume.coerceIn(0f, 1f),
                            isSilent = avgVolume < 0.01f,
                            speechConfidence = (avgVolume * 3f).coerceIn(0f, 1f)
                        )
                    )
                    currentSegmentStart = timeUs
                    totalAmplitude = 0.0
                    sampleCount = 0
                }

                buffer.clear()
                extractor.advance()
            }

            try { extractor.release() } catch (_: Exception) {}
        } catch (e: Exception) {
            // Return what we have
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
}
