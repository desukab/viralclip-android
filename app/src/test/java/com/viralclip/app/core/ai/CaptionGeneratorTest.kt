package com.viralclip.app.core.ai

import android.content.Context
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.domain.model.CaptionPreset
import com.viralclip.app.domain.model.CaptionSegment
import com.viralclip.app.domain.model.CaptionWord
import com.viralclip.app.domain.model.CaseStyle
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

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var generator: CaptionGenerator
    private val fakeUri: Uri = mockk(relaxed = true)

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
        unmockkAll()
    }

    @Test
    fun `generator initializes with progress at zero`() {
        assertEquals(0f, generator.progress.value, 0.01f)
    }

    @Test
    fun `transcription result data class works`() {
        val result = CaptionGenerator.TranscriptionResult(
            segments = emptyList(),
            language = "en",
            totalWords = 0,
            durationMs = 5000L
        )
        assertTrue(result.segments.isEmpty())
        assertEquals("en", result.language)
        assertEquals(0, result.totalWords)
        assertEquals(5000L, result.durationMs)
    }

    @Test
    fun `TranscriptionResult counts words correctly`() {
        val segments = listOf(
            CaptionSegment(text = "Hello world", startTimeMs = 0L, endTimeMs = 1000L),
            CaptionSegment(text = "This is a test", startTimeMs = 1000L, endTimeMs = 2000L)
        )
        val result = CaptionGenerator.TranscriptionResult(
            segments = segments,
            language = "en",
            totalWords = segments.sumOf { it.text.split(" ").size },
            durationMs = 2000L
        )
        assertEquals(6, result.totalWords)
    }

    @Test
    fun `splitIntoSegments with empty text returns empty list`() {
        val result = generator.splitIntoSegments("", 1000L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `splitIntoSegments with whitespace text returns empty list`() {
        val result = generator.splitIntoSegments("   \t  ", 1000L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `splitIntoSegments divides text into time-aligned segments`() {
        val text = "one two three four five six seven eight nine ten"
        val result = generator.splitIntoSegments(text, 10000L, maxWordsPerSegment = 3)

        assertTrue(result.isNotEmpty())
        result.forEach { segment ->
            assertTrue(segment.startTimeMs >= 0L)
            assertTrue(segment.endTimeMs <= 10000L)
            assertTrue(segment.endTimeMs >= segment.startTimeMs)
        }
    }

    @Test
    fun `splitIntoSegments respects maxWordsPerSegment limit`() {
        val text = "alpha beta gamma delta epsilon zeta"
        val result = generator.splitIntoSegments(text, 6000L, maxWordsPerSegment = 2)

        result.forEach { segment ->
            val wordCount = segment.text.split(" ").size
            assertTrue("Segment has $wordCount words, expected <= 2", wordCount <= 2)
        }
    }

    @Test
    fun `splitIntoSegments sets confidence to high value`() {
        val result = generator.splitIntoSegments("hello world", 2000L)
        assertTrue(result.isNotEmpty())
        result.forEach { segment ->
            assertEquals(0.95f, segment.confidence, 0.001f)
        }
    }

    @Test
    fun `formatCaptionText with NORMAL keeps original case`() {
        val result = generator.formatCaptionText("Hello World", CaseStyle.NORMAL)
        assertEquals("Hello World", result)
    }

    @Test
    fun `formatCaptionText with UPPERCASE converts to all caps`() {
        val result = generator.formatCaptionText("Hello World", CaseStyle.UPPERCASE)
        assertEquals("HELLO WORLD", result)
    }

    @Test
    fun `formatCaptionText with LOWERCASE converts to all lower`() {
        val result = generator.formatCaptionText("Hello World", CaseStyle.LOWERCASE)
        assertEquals("hello world", result)
    }

    @Test
    fun `formatCaptionText with TITLE_CASE capitalizes each word`() {
        val result = generator.formatCaptionText("hello world test", CaseStyle.TITLE_CASE)
        assertEquals("Hello World Test", result)
    }

    @Test
    fun `formatCaptionText with FIRST_WORD_CAPS only capitalizes first character`() {
        val result = generator.formatCaptionText("hello world", CaseStyle.FIRST_WORD_CAPS)
        assertEquals("Hello world", result)
    }

    @Test
    fun `formatCaptionText word-wraps when exceeding maxChars`() {
        val longText = "This is a very long caption that should be wrapped onto multiple lines"
        val result = generator.formatCaptionText(longText, CaseStyle.NORMAL, maxChars = 20)
        assertTrue("Result should contain newlines for wrapping", result.contains("\n"))
    }

    @Test
    fun `formatCaptionText does not wrap short text`() {
        val shortText = "Hello"
        val result = generator.formatCaptionText(shortText, CaseStyle.NORMAL, maxChars = 100)
        assertFalse("Short text should not be wrapped", result.contains("\n"))
    }

    @Test
    fun `getHighlightedWords with empty highlight list and autoHighlight returns flags`() {
        val result = generator.getHighlightedWords(
            "this is amazing and crazy",
            highlightWords = emptyList(),
            autoHighlight = true
        )
        assertEquals(5, result.size)
    }

    @Test
    fun `getHighlightedWords with explicit list highlights matching words`() {
        val result = generator.getHighlightedWords(
            "hello world hello",
            highlightWords = listOf("hello"),
            autoHighlight = false
        )
        assertEquals(3, result.size)
        assertTrue(result[0].second)
        assertFalse(result[1].second)
        assertTrue(result[2].second)
    }

    @Test
    fun `getHighlightedWords auto-highlights power words`() {
        val result = generator.getHighlightedWords(
            "this is amazing",
            highlightWords = emptyList(),
            autoHighlight = true
        )
        val amazingHighlighted = result.find { it.first.lowercase() == "amazing" }
        assertNotNull(amazingHighlighted)
        assertTrue(amazingHighlighted!!.second)
    }

    @Test
    fun `getHighlightedWords non-power words not highlighted`() {
        val result = generator.getHighlightedWords(
            "the quick brown fox",
            highlightWords = emptyList(),
            autoHighlight = true
        )
        val theHighlighted = result.find { it.first.lowercase() == "the" }
        assertNotNull(theHighlighted)
        assertFalse(theHighlighted!!.second)
    }

    @Test
    fun `optimizeSegments with empty list returns empty list`() {
        val result = generator.optimizeSegments(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `optimizeSegments merges short consecutive segments`() {
        val segments = listOf(
            CaptionSegment(text = "Hi", startTimeMs = 0L, endTimeMs = 500L),
            CaptionSegment(text = "there", startTimeMs = 500L, endTimeMs = 1500L)
        )
        val result = generator.optimizeSegments(segments, minDurationMs = 1000, maxDurationMs = 5000)
        assertTrue("Should merge into fewer segments", result.size < segments.size)
    }

    @Test
    fun `optimizeSegments preserves long-enough segments`() {
        val segments = listOf(
            CaptionSegment(text = "First caption", startTimeMs = 0L, endTimeMs = 2000L),
            CaptionSegment(text = "Second caption", startTimeMs = 3000L, endTimeMs = 5000L)
        )
        val result = generator.optimizeSegments(segments)
        assertEquals(2, result.size)
    }

    @Test
    fun `optimizeSegments handles single segment`() {
        val segments = listOf(
            CaptionSegment(text = "Only one", startTimeMs = 0L, endTimeMs = 2000L)
        )
        val result = generator.optimizeSegments(segments)
        assertEquals(1, result.size)
        assertEquals("Only one", result[0].text)
    }

    @Test
    fun `generateWordTimestamps produces ordered word-level timings`() {
        val segment = CaptionSegment(
            text = "Hello world this is a test",
            startTimeMs = 0L,
            endTimeMs = 6000L
        )
        val timings = generator.generateWordTimestamps(listOf(segment))

        assertEquals(7, timings.size)
        timings.forEach { (word, seg) ->
            assertEquals(segment.text, seg.text)
            assertTrue(word.startTimeMs >= 0)
            assertTrue(word.endTimeMs <= 6000L)
        }
    }

    @Test
    fun `generateWordTimestamps with empty list returns empty`() {
        val result = generator.generateWordTimestamps(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `generateWordTimestamps with multiple segments aggregates correctly`() {
        val segments = listOf(
            CaptionSegment(text = "First segment here", startTimeMs = 0L, endTimeMs = 3000L),
            CaptionSegment(text = "Second segment text", startTimeMs = 3000L, endTimeMs = 6000L)
        )
        val result = generator.generateWordTimestamps(segments)
        assertEquals(6, result.size)
    }

    @Test
    fun `generateCaptions with no audio returns empty result`() = runTest(testDispatcher) {
        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(
            sampleRate = 44100, channels = 1, bitrate = 128000,
            durationMs = 5000L, format = "aac"
        )
        every { audioProcessor.analyzeAudioSegments(any(), any(), any()) } returns emptyList()

        val result = generator.generateCaptions(fakeUri, "en")

        assertEquals(0, result.segments.size)
        assertEquals(0, result.totalWords)
        assertEquals("en", result.language)
    }

    @Test
    fun `generateCaptions processes silent segments to empty captions`() = runTest(testDispatcher) {
        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(
            sampleRate = 44100, channels = 1, bitrate = 128000,
            durationMs = 10000L, format = "aac"
        )
        val silentSegments = listOf(
            AudioProcessor.AudioSegment(0L, 1000L, 0.001f, true, 0f),
            AudioProcessor.AudioSegment(1000L, 2000L, 0.001f, true, 0f),
            AudioProcessor.AudioSegment(2000L, 3000L, 0.001f, true, 0f)
        )
        every { audioProcessor.analyzeAudioSegments(any(), any(), any()) } returns silentSegments

        val result = generator.generateCaptions(fakeUri, "en")

        assertEquals(0, result.segments.size)
    }

    @Test
    fun `generateCaptions groups contiguous speech blocks`() = runTest(testDispatcher) {
        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(
            sampleRate = 44100, channels = 1, bitrate = 128000,
            durationMs = 10000L, format = "aac"
        )
        val speechSegments = listOf(
            AudioProcessor.AudioSegment(0L, 1000L, 0.3f, false, 0.7f),
            AudioProcessor.AudioSegment(1000L, 2000L, 0.4f, false, 0.8f),
            AudioProcessor.AudioSegment(2000L, 3000L, 0.5f, false, 0.9f),
            AudioProcessor.AudioSegment(10000L, 11000L, 0.3f, false, 0.7f)
        )
        every { audioProcessor.analyzeAudioSegments(any(), any(), any()) } returns speechSegments

        val result = generator.generateCaptions(fakeUri, "en")

        assertTrue(result.segments.isNotEmpty())
    }

    @Test
    fun `progress is updated during generation`() = runTest(testDispatcher) {
        every { audioProcessor.getAudioInfo(any()) } returns AudioProcessor.AudioInfo(
            sampleRate = 44100, channels = 1, bitrate = 128000,
            durationMs = 5000L, format = "aac"
        )
        every { audioProcessor.analyzeAudioSegments(any(), any(), any()) } returns listOf(
            AudioProcessor.AudioSegment(0L, 1000L, 0.3f, false, 0.7f)
        )

        val result = generator.generateCaptions(fakeUri, "en") { progress ->
            assertTrue("Progress should be in 0-1 range: $progress", progress in 0f..1f)
        }
        assertEquals(1f, generator.progress.value, 0.001f)
    }
}
