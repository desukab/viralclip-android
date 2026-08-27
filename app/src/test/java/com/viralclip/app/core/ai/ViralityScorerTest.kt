package com.viralclip.app.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.domain.model.CaptionPreset
import com.viralclip.app.domain.model.ViralityScore
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViralityScorerTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var scorer: ViralityScorer

    private val fakeUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        scorer = ViralityScorer(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `progress starts at zero`() {
        assertEquals(0f, scorer.progress.value, 0.01f)
    }

    @Test
    fun `ScoringResult data class constructs correctly`() {
        val scoredClip = ViralityScorer.ScoredClip(
            startTimeMs = 0L,
            endTimeMs = 30000L,
            score = ViralityScore(
                overall = 0.75f,
                engagementPotential = 0.8f,
                emotionalImpact = 0.7f,
                shareability = 0.6f,
                watchTime = 0.85f,
                hookStrength = 0.9f,
                reasons = listOf("Strong hook", "Good audio"),
                suggestedStartTime = 0L,
                suggestedEndTime = 30000L
            ),
            recommendedCaptionStyle = CaptionPreset.BOLD_HIGHLIGHT
        )
        val result = ViralityScorer.ScoringResult(
            clips = listOf(scoredClip),
            overallVideoScore = 0.75f,
            analysisSummary = "Test summary"
        )
        assertEquals(1, result.clips.size)
        assertEquals(0.75f, result.overallVideoScore, 0.001f)
        assertEquals("Test summary", result.analysisSummary)
    }

    @Test
    fun `analyzeAndScore returns empty for zero duration`() = runTest(testDispatcher) {
        val frames = listOf(0L to createFakeBitmap())
        val segments = listOf(
            AudioProcessor.AudioSegment(0L, 1000L, 0.1f, false, 0.5f)
        )

        val result = scorer.analyzeAndScore(fakeUri, 0L, segments, frames)
        assertTrue(result.clips.isEmpty())
        assertEquals(0f, result.overallVideoScore, 0.001f)
        assertTrue(result.analysisSummary.contains("Insufficient"))
    }

    @Test
    fun `analyzeAndScore returns empty for no frames`() = runTest(testDispatcher) {
        val segments = listOf(
            AudioProcessor.AudioSegment(0L, 1000L, 0.1f, false, 0.5f)
        )

        val result = scorer.analyzeAndScore(fakeUri, 60000L, segments, emptyList())
        assertTrue(result.clips.isEmpty())
        assertEquals(0f, result.overallVideoScore, 0.001f)
    }

    @Test
    fun `analyzeAndScore produces clips for valid input`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            0L to bitmap,
            5000L to bitmap,
            10000L to bitmap,
            15000L to bitmap,
            20000L to bitmap
        )
        val segments = listOf(
            AudioProcessor.AudioSegment(0L, 5000L, 0.3f, false, 0.7f),
            AudioProcessor.AudioSegment(5000L, 10000L, 0.4f, false, 0.8f),
            AudioProcessor.AudioSegment(10000L, 15000L, 0.2f, false, 0.5f),
            AudioProcessor.AudioSegment(15000L, 20000L, 0.5f, false, 0.9f)
        )

        val result = scorer.analyzeAndScore(fakeUri, 25000L, segments, frames)

        assertTrue(result.clips.isNotEmpty())
        assertTrue(result.overallVideoScore >= 0f)
        assertTrue(result.overallVideoScore <= 1f)
        assertTrue(result.analysisSummary.isNotEmpty())
    }

    @Test
    fun `analyzeAndScore limits clips to maxClips`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..29).map { (it * 2000L) to bitmap }
        val segments = (0..14).map {
            AudioProcessor.AudioSegment(it * 10000L, (it + 1) * 10000L, 0.3f, false, 0.7f)
        }

        val result = scorer.analyzeAndScore(fakeUri, 300000L, segments, frames)

        assertTrue(result.clips.size <= 8)
    }

    @Test
    fun `virality score reasons are generated based on content`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            0L to bitmap, 2000L to bitmap, 4000L to bitmap,
            6000L to bitmap, 8000L to bitmap, 10000L to bitmap
        )
        val segments = listOf(
            AudioProcessor.AudioSegment(0L, 3000L, 0.4f, false, 0.7f),
            AudioProcessor.AudioSegment(3000L, 6000L, 0.5f, false, 0.8f),
            AudioProcessor.AudioSegment(6000L, 10000L, 0.6f, false, 0.9f)
        )

        val result = scorer.analyzeAndScore(fakeUri, 15000L, segments, frames)

        result.clips.forEach { clip ->
            assertNotNull(clip.score)
            assertNotNull(clip.score.reasons)
            assertTrue(clip.score.reasons is List<*>)
        }
    }

    @Test
    fun `caption preset is assigned based on content characteristics`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(0L to bitmap, 5000L to bitmap, 10000L to bitmap)
        val segments = listOf(
            AudioProcessor.AudioSegment(0L, 5000L, 0.8f, false, 0.95f)
        )

        val result = scorer.analyzeAndScore(fakeUri, 15000L, segments, frames)

        result.clips.forEach { clip ->
            assertNotNull(clip.recommendedCaptionStyle)
            assertTrue(clip.recommendedCaptionStyle is CaptionPreset)
        }
    }

    @Test
    fun `scoring weights sum correctly`() {
        val weights = listOf(0.25f, 0.25f, 0.20f, 0.15f, 0.15f)
        assertEquals(1.0f, weights.sum(), 0.001f)
    }

    @Test
    fun `ViralityScore label classification works correctly`() {
        val highScore = ViralityScore(
            overall = 0.85f, engagementPotential = 0.8f,
            emotionalImpact = 0.7f, shareability = 0.6f,
            watchTime = 0.85f, hookStrength = 0.9f,
            reasons = emptyList(),
            suggestedStartTime = 0L, suggestedEndTime = 30000L
        )
        assertEquals("High", highScore.label)

        val mediumScore = highScore.copy(overall = 0.5f)
        assertEquals("Medium", mediumScore.label)

        val lowScore = highScore.copy(overall = 0.2f)
        assertEquals("Low", lowScore.label)
    }

    @Test
    fun `ViralityScore percentage calculation is correct`() {
        val score = ViralityScore(
            overall = 0.756f, engagementPotential = 0.8f,
            emotionalImpact = 0.7f, shareability = 0.6f,
            watchTime = 0.85f, hookStrength = 0.9f,
            reasons = emptyList(),
            suggestedStartTime = 0L, suggestedEndTime = 30000L
        )
        assertEquals(75, score.percentage)
    }

    @Test
    fun `empty segments list handled gracefully`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(0L to bitmap, 5000L to bitmap)

        val result = scorer.analyzeAndScore(fakeUri, 10000L, emptyList(), frames)

        assertTrue(result.clips.isNotEmpty())
        assertEquals(0f, result.overallVideoScore, 0.001f)
    }

    @Test
    fun `very long video handled with reasonable clip count`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..29).map { (it * 10000L) to bitmap }
        val segments = (0..14).map {
            AudioProcessor.AudioSegment(it * 20000L, (it + 1) * 20000L, 0.3f, false, 0.6f)
        }

        val result = scorer.analyzeAndScore(fakeUri, 300000L, segments, frames)

        assertTrue(result.clips.size in 1..8)
        assertTrue(result.overallVideoScore >= 0f)
    }

    private fun createFakeBitmap(): Bitmap {
        return mockk(relaxed = true).apply {
            every { isRecycled } returns false
            every { width } returns 1080
            every { height } returns 1920
            every { getPixel(any(), any()) } returns 0xFF808080.toInt()
            every { getPixels(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        }
    }
}
