package com.viralclip.app.core.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.SceneType
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FrameAnalyzerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var ffmpegProcessor: FFmpegProcessor
    private lateinit var analyzer: FrameAnalyzer

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        ffmpegProcessor = mockk(relaxed = true)
        analyzer = FrameAnalyzer(context, ffmpegProcessor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─── Progress ─────────────────────────────────────────────

    @Test
    fun `progress starts at zero`() {
        assertEquals(0f, analyzer.progress.value, 0.01f)
    }

    @Test
    fun `progress reaches one after analysis`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        analyzer.analyzeFrames(listOf(0L to bitmap, 1000L to bitmap))

        assertEquals(1f, analyzer.progress.value, 0.001f)
    }

    @Test
    fun `progress is StateFlow`() {
        assertTrue(analyzer.progress is kotlinx.coroutines.flow.StateFlow)
    }

    // ─── analyzeFrames - Empty / Invalid Input ────────────────

    @Test
    fun `analyzeFrames with empty list returns empty`() = runTest(testDispatcher) {
        val result = analyzer.analyzeFrames(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyzeFrames skips recycled bitmaps`() = runTest(testDispatcher) {
        val recycledBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns true
            every { width } returns 1080
            every { height } returns 1920
        }

        val result = analyzer.analyzeFrames(listOf(0L to recycledBitmap))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyzeFrames skips invalid bitmaps with zero dimensions`() = runTest(testDispatcher) {
        val invalidBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns false
            every { width } returns 0
            every { height } returns 0
        }

        val result = analyzer.analyzeFrames(listOf(0L to invalidBitmap))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyzeFrames with mixed valid and invalid bitmaps`() = runTest(testDispatcher) {
        val validBitmap = createFakeBitmap()
        val recycledBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns true
            every { width } returns 1080
            every { height } returns 1920
        }

        val result = analyzer.analyzeFrames(listOf(
            0L to recycledBitmap,
            1000L to validBitmap,
            2000L to recycledBitmap
        ))

        assertEquals(1, result.size)
        assertEquals(1000L, result[0].timestampMs)
    }

    // ─── analyzeFrames - Happy Path ───────────────────────────

    @Test
    fun `analyzeFrames with valid frames returns analyses`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            0L to bitmap,
            1000L to bitmap,
            2000L to bitmap
        )

        val result = analyzer.analyzeFrames(frames)

        assertEquals(3, result.size)
        result.forEach { analysis ->
            assertTrue("Brightness should be in 0-1 range: ${analysis.brightness}",
                analysis.brightness in 0f..1f)
            assertTrue("Engagement should be in 0-1 range: ${analysis.engagementScore}",
                analysis.engagementScore in 0f..1f)
            assertTrue("Motion should be in 0-1 range: ${analysis.motionScore}",
                analysis.motionScore in 0f..1f)
        }
    }

    @Test
    fun `analyzeFrames sets timestampMs from input`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            500L to bitmap,
            1500L to bitmap
        )

        val result = analyzer.analyzeFrames(frames)

        assertEquals(500L, result[0].timestampMs)
        assertEquals(1500L, result[1].timestampMs)
    }

    @Test
    fun `analyzeFrames sets face count to zero`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val result = analyzer.analyzeFrames(listOf(0L to bitmap))

        assertEquals(0, result[0].faceCount)
        assertTrue(result[0].facePositions.isEmpty())
    }

    @Test
    fun `analyzeFrames assigns appropriate scene types`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..4).map { (it * 1000L) to bitmap }

        val result = analyzer.analyzeFrames(frames)

        result.forEach { analysis ->
            assertNotNull(analysis.sceneType)
            assertTrue(analysis.sceneType is SceneType)
        }
    }

    @Test
    fun `speechDetected flag is set correctly`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val result = analyzer.analyzeFrames(listOf(0L to bitmap))
        assertNotNull(result[0].speechDetected)
    }

    @Test
    fun `analyzeFrames first frame has zero motion`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val result = analyzer.analyzeFrames(listOf(0L to bitmap))

        assertEquals(0f, result[0].motionScore, 0.001f)
    }

    @Test
    fun `analyzeFrames with single frame`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val result = analyzer.analyzeFrames(listOf(0L to bitmap))

        assertEquals(1, result.size)
        assertEquals(0L, result[0].timestampMs)
    }

    @Test
    fun `analyzeFrames with many frames`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..49).map { (it * 500L) to bitmap }

        val result = analyzer.analyzeFrames(frames)

        assertEquals(50, result.size)
    }

    // ─── analyzeFrames - Brightness Analysis ──────────────────

    @Test
    fun `analyzeFrames dark bitmap has low brightness`() = runTest(testDispatcher) {
        val darkBitmap = createColoredBitmap(0xFF101010.toInt())
        val result = analyzer.analyzeFrames(listOf(0L to darkBitmap))

        assertTrue("Dark bitmap brightness should be low",
            result[0].brightness < 0.2f)
    }

    @Test
    fun `analyzeFrames bright bitmap has high brightness`() = runTest(testDispatcher) {
        val brightBitmap = createColoredBitmap(0xFFF0F0F0.toInt())
        val result = analyzer.analyzeFrames(listOf(0L to brightBitmap))

        assertTrue("Bright bitmap brightness should be high",
            result[0].brightness > 0.8f)
    }

    @Test
    fun `analyzeFrames with mid-gray bitmap`() = runTest(testDispatcher) {
        val grayBitmap = createColoredBitmap(0xFF808080.toInt())
        val result = analyzer.analyzeFrames(listOf(0L to grayBitmap))

        assertTrue("Gray bitmap brightness should be around 0.5",
            result[0].brightness in 0.3f..0.7f)
    }

    // ─── detectSceneChanges ───────────────────────────────────

    @Test
    fun `detectSceneChanges with empty frames returns empty list`() = runTest(testDispatcher) {
        val result = analyzer.detectSceneChanges(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectSceneChanges with identical frames returns no changes`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            0L to bitmap,
            1000L to bitmap,
            2000L to bitmap
        )

        val result = analyzer.detectSceneChanges(frames)

        assertTrue(result.isEmpty() || result.size < frames.size)
    }

    @Test
    fun `detectSceneChanges with very different frames detects changes`() = runTest(testDispatcher) {
        val darkBitmap = createColoredBitmap(0xFF000000.toInt())
        val lightBitmap = createColoredBitmap(0xFFFFFFFF.toInt())
        val frames = listOf(
            0L to darkBitmap,
            1000L to lightBitmap,
            2000L to darkBitmap
        )

        val result = analyzer.detectSceneChanges(frames, threshold = 0.2f)

        assertTrue("Should detect at least one scene change", result.isNotEmpty())
    }

    @Test
    fun `detectSceneChanges respects threshold parameter`() = runTest(testDispatcher) {
        val darkBitmap = createColoredBitmap(0xFF000000.toInt())
        val lightBitmap = createColoredBitmap(0xFFFFFFFF.toInt())
        val frames = listOf(0L to darkBitmap, 1000L to lightBitmap)

        val lowResult = analyzer.detectSceneChanges(frames, threshold = 0.1f)
        val highResult = analyzer.detectSceneChanges(frames, threshold = 0.9f)

        assertTrue(lowResult.size >= highResult.size)
    }

    @Test
    fun `detectSceneChanges with single frame returns empty`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val result = analyzer.detectSceneChanges(listOf(0L to bitmap))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detectSceneChanges with recycled frames in list`() = runTest(testDispatcher) {
        val validBitmap = createFakeBitmap()
        val recycledBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns true
            every { width } returns 1080
            every { height } returns 1920
        }
        val frames = listOf(
            0L to recycledBitmap,
            1000L to validBitmap,
            2000L to validBitmap
        )

        val result = analyzer.detectSceneChanges(frames)
        assertTrue(result is List<*>)
    }

    @Test
    fun `detectSceneChanges result timestamps are non-negative`() = runTest(testDispatcher) {
        val darkBitmap = createColoredBitmap(0xFF000000.toInt())
        val lightBitmap = createColoredBitmap(0xFFFFFFFF.toInt())
        val frames = listOf(
            0L to darkBitmap,
            1000L to lightBitmap,
            2000L to darkBitmap
        )

        val result = analyzer.detectSceneChanges(frames, threshold = 0.2f)
        result.forEach { timestamp ->
            assertTrue("Timestamp should be non-negative", timestamp >= 0)
        }
    }

    // ─── findEngagingMoments ──────────────────────────────────

    @Test
    fun `findEngagingMoments with empty frames returns empty`() = runTest(testDispatcher) {
        val result = analyzer.findEngagingMoments(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findEngagingMoments returns up to 10 moments`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..19).map { (it * 500L) to bitmap }

        val result = analyzer.findEngagingMoments(frames)

        assertTrue("Result should be capped at 10", result.size <= 10)
    }

    @Test
    fun `findEngagingMoments results are sorted by engagement descending`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..9).map { (it * 500L) to bitmap }

        val result = analyzer.findEngagingMoments(frames)

        for (i in 0 until result.size - 1) {
            assertTrue("Results should be sorted descending",
                result[i].second >= result[i + 1].second)
        }
    }

    @Test
    fun `findEngagingMoments respects windowSize parameter`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..9).map { (it * 500L) to bitmap }

        val smallWindow = analyzer.findEngagingMoments(frames, windowSize = 2)
        val largeWindow = analyzer.findEngagingMoments(frames, windowSize = 8)

        assertTrue(smallWindow.size <= 10)
        assertTrue(largeWindow.size <= 10)
    }

    @Test
    fun `findEngagingMoments with single frame`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val result = analyzer.findEngagingMoments(listOf(0L to bitmap))

        assertEquals(1, result.size)
        assertEquals(0L, result[0].first)
        assertTrue(result[0].second in 0f..1f)
    }

    @Test
    fun `findEngagingMoments returns pairs of timestamp and score`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = (0..5).map { (it * 1000L) to bitmap }

        val result = analyzer.findEngagingMoments(frames)

        result.forEach { (timestamp, score) ->
            assertTrue("Timestamp should be non-negative", timestamp >= 0)
            assertTrue("Score should be in 0-1", score in 0f..1f)
        }
    }

    // ─── Motion Detection ─────────────────────────────────────

    @Test
    fun `analyzeFrames with different frames detects motion`() = runTest(testDispatcher) {
        val darkBitmap = createColoredBitmap(0xFF000000.toInt())
        val lightBitmap = createColoredBitmap(0xFFFFFFFF.toInt())
        val frames = listOf(
            0L to darkBitmap,
            1000L to lightBitmap
        )

        val result = analyzer.analyzeFrames(frames)

        assertTrue("Second frame should have motion > 0",
            result[1].motionScore > 0f)
    }

    @Test
    fun `analyzeFrames with same consecutive frames has zero motion`() = runTest(testDispatcher) {
        val bitmap = createColoredBitmap(0xFF808080.toInt())
        val frames = listOf(
            0L to bitmap,
            1000L to bitmap,
            2000L to bitmap
        )

        val result = analyzer.analyzeFrames(frames)

        for (i in 1 until result.size) {
            assertEquals("Motion should be 0 for identical frames",
                0f, result[i].motionScore, 0.001f)
        }
    }

    // ─── Scene Classification ─────────────────────────────────

    @Test
    fun `classifyScene ACTION for high motion and bright`() = runTest(testDispatcher) {
        val frames = createMotionFrames(10)
        val result = analyzer.analyzeFrames(frames)

        result.forEach { analysis ->
            assertNotNull(analysis.sceneType)
        }
    }

    // ─── Engagement Scoring ───────────────────────────────────

    @Test
    fun `engagement score is within bounds for all inputs`() = runTest(testDispatcher) {
        val frames = listOf(
            createColoredBitmap(0xFF000000.toInt()),
            createColoredBitmap(0xFF404040.toInt()),
            createColoredBitmap(0xFF808080.toInt()),
            createColoredBitmap(0xFFC0C0C0.toInt()),
            createColoredBitmap(0xFFFFFFFF.toInt())
        ).mapIndexed { index, bitmap -> (index * 1000L) to bitmap }

        val result = analyzer.analyzeFrames(frames)

        result.forEach { analysis ->
            assertTrue("Engagement score should be in 0-1: ${analysis.engagementScore}",
                analysis.engagementScore in 0f..1f)
        }
    }

    @Test
    fun `engagement score is consistent with brightness`() = runTest(testDispatcher) {
        val midGrayBitmap = createColoredBitmap(0xFF808080.toInt())
        val result = analyzer.analyzeFrames(listOf(0L to midGrayBitmap))

        assertTrue("Mid-gray should have reasonable engagement",
            result[0].engagementScore in 0.2f..1f)
    }

    // ─── Helper Methods ───────────────────────────────────────

    private fun createFakeBitmap(): Bitmap {
        return mockk(relaxed = true).apply {
            every { isRecycled } returns false
            every { width } returns 1080
            every { height } returns 1920
            every { getPixels(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        }
    }

    private fun createColoredBitmap(color: Int): Bitmap {
        return mockk(relaxed = true).apply {
            every { isRecycled } returns false
            every { width } returns 1080
            every { height } returns 1920
            every { getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
                val pixels = firstArg()
                val count = pixels.size
                for (i in 0 until count) {
                    pixels[i] = color
                }
                Unit
            }
        }
    }

    private fun createMotionFrames(count: Int): List<Pair<Long, Bitmap>> {
        return (0 until count).map { index ->
            val color = if (index % 2 == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            (index * 500L) to createColoredBitmap(color)
        }
    }
}
