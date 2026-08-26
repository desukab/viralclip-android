package com.viralclip.app.core.ai

import android.content.Context
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptionGeneratorTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var generator: CaptionGenerator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        audioProcessor = mockk(relaxed = true)
        generator = CaptionGenerator(context, audioProcessor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generate captions from speech segments produces non-empty result`() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        val segments = listOf(
            AudioProcessor.AudioSegment(startTimeMs = 0L, endTimeMs = 2000L, volume = 0.8f, isSilent = false, speechConfidence = 0.9f),
            AudioProcessor.AudioSegment(startTimeMs = 2100L, endTimeMs = 4000L, volume = 0.7f, isSilent = false, speechConfidence = 0.85f)
        )

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 8000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns segments

        val result = generator.generateCaptions(uri, "en")

        assertNotNull("Result should not be null", result)
        assertTrue("Duration should be positive", result.durationMs > 0)
    }

    @Test
    fun `generate captions from silent audio handles gracefully`() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        val silentSegments = listOf(
            AudioProcessor.AudioSegment(startTimeMs = 0L, endTimeMs = 2000L, volume = 0.01f, isSilent = true, speechConfidence = 0.05f)
        )

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 4000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns silentSegments

        val result = generator.generateCaptions(uri, "en")

        assertNotNull("Result should not be null", result)
    }

    @Test
    fun `generate captions from empty segments produces empty result`() = runTest {
        val uri = mockk<Uri>(relaxed = true)

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 5000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns emptyList()

        val result = generator.generateCaptions(uri, "en")

        assertNotNull("Result should not be null", result)
        assertTrue("Empty segments should produce empty list", result.segments.isEmpty())
    }

    @Test
    fun `transcription result has correct duration`() = runTest {
        val uri = mockk<Uri>(relaxed = true)
        val segments = listOf(
            AudioProcessor.AudioSegment(startTimeMs = 1000L, endTimeMs = 3000L, volume = 0.5f, isSilent = false, speechConfidence = 0.7f)
        )

        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(durationMs = 5000L, sampleRate = 44100, channels = 1, bitrate = 128000)
        coEvery { audioProcessor.analyzeAudioSegments(any()) } returns segments

        val result = generator.generateCaptions(uri, "en")

        assertEquals("Duration should match audio info", 5000L, result.durationMs)
    }
}
