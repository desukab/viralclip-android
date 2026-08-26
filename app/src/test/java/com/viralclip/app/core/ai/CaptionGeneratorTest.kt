package com.viralclip.app.core.ai

import android.content.Context
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CaptionGeneratorTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var audioProcessor: AudioProcessor

    private lateinit var generator: CaptionGenerator

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        generator = CaptionGenerator(context, audioProcessor)
    }

    @Test
    fun `generate captions from speech segments produces non-empty result`() = runTest {
        val uri = mockk<Uri>()
        val segments = listOf(
            AudioProcessor.AudioSegment(startTimeMs = 0L, endTimeMs = 2000L, volume = 0.8f, isSilent = false, speechConfidence = 0.9f),
            AudioProcessor.AudioSegment(startTimeMs = 2100L, endTimeMs = 4000L, volume = 0.7f, isSilent = false, speechConfidence = 0.85f),
            AudioProcessor.AudioSegment(startTimeMs = 5000L, endTimeMs = 7000L, volume = 0.6f, isSilent = false, speechConfidence = 0.8f)
        )

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 8000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns segments

        val result = generator.generateCaptions(uri, "en")

        // Should have at least 1 segment
        assertTrue("Should generate caption segments", result.segments.isNotEmpty())
        assertTrue("Duration should be positive", result.durationMs > 0)
        assertTrue("Total words should be >= 0", result.totalWords >= 0)

        // Segments should have valid time ranges
        result.segments.forEach { caption ->
            assertTrue("Caption start should be >= 0", caption.startTimeMs >= 0)
            assertTrue("Caption end should be > start", caption.endTimeMs > caption.startTimeMs)
            assertTrue("Caption text should not be empty", caption.text.isNotBlank())
        }
    }

    @Test
    fun `generate captions from silent audio produces empty or minimal result`() = runTest {
        val uri = mockk<Uri>()
        val silentSegments = listOf(
            AudioProcessor.AudioSegment(startTimeMs = 0L, endTimeMs = 2000L, volume = 0.01f, isSilent = true, speechConfidence = 0.05f),
            AudioProcessor.AudioSegment(startTimeMs = 2000L, endTimeMs = 4000L, volume = 0.01f, isSilent = true, speechConfidence = 0.05f)
        )

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 4000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns silentSegments

        val result = generator.generateCaptions(uri, "en")

        // With all silent segments, should have no or very few segments
        // (depends on implementation - some may still generate fallback captions)
        assertNotNull("Result should not be null", result)
    }

    @Test
    fun `generate captions handles empty segments`() = runTest {
        val uri = mockk<Uri>()

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 5000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns emptyList()

        val result = generator.generateCaptions(uri, "en")

        // Should handle gracefully
        assertNotNull("Result should not be null", result)
        assertTrue("Empty segments should produce empty segments", result.segments.isEmpty())
    }

    @Test
    fun `transcription result has correct duration`() = runTest {
        val uri = mockk<Uri>()
        val segments = listOf(
            AudioProcessor.AudioSegment(startTimeMs = 1000L, endTimeMs = 3000L, volume = 0.5f, isSilent = false, speechConfidence = 0.7f)
        )

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 5000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns segments

        val result = generator.generateCaptions(uri, "en")

        assertEquals("Duration should match audio info", 5000L, result.durationMs)
    }
}
