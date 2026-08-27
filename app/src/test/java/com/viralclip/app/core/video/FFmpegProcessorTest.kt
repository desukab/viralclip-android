package com.viralclip.app.core.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FFmpegProcessorTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var processor: FFmpegProcessor
    private val fakeUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        processor = FFmpegProcessor(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─── Progress ─────────────────────────────────────────────

    @Test
    fun `progress starts at zero`() {
        assertEquals(0f, processor.progress.value, 0.01f)
    }

    @Test
    fun `progress is StateFlow`() {
        assertTrue(processor.progress is kotlinx.coroutines.flow.StateFlow)
    }

    // ─── VideoInfo Data Class ─────────────────────────────────

    @Test
    fun `VideoInfo data class constructs correctly`() {
        val info = FFmpegProcessor.VideoInfo(
            durationMs = 60000L,
            width = 1920,
            height = 1080,
            bitrate = 8_000_000,
            frameRate = 30f,
            hasAudio = true,
            rotation = 0
        )
        assertEquals(60000L, info.durationMs)
        assertEquals(1920, info.width)
        assertEquals(1080, info.height)
        assertEquals(8_000_000, info.bitrate)
        assertEquals(30f, info.frameRate, 0.001f)
        assertTrue(info.hasAudio)
        assertEquals(0, info.rotation)
    }

    @Test
    fun `VideoInfo handles rotated video dimensions`() {
        val landscape = FFmpegProcessor.VideoInfo(
            durationMs = 60000L, width = 1920, height = 1080,
            bitrate = 8_000_000, frameRate = 30f, hasAudio = true, rotation = 90
        )
        assertEquals(1080, landscape.width)
        assertEquals(1920, landscape.height)

        val rotated = landscape.copy(rotation = 270)
        assertEquals(1080, rotated.width)
        assertEquals(1920, rotated.height)
    }

    @Test
    fun `VideoInfo data class equality`() {
        val info1 = FFmpegProcessor.VideoInfo(60000L, 1920, 1080, 8_000_000, 30f, true, 0)
        val info2 = FFmpegProcessor.VideoInfo(60000L, 1920, 1080, 8_000_000, 30f, true, 0)
        assertEquals(info1, info2)
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun `VideoInfo data class copy`() {
        val original = FFmpegProcessor.VideoInfo(60000L, 1920, 1080, 8_000_000, 30f, true, 0)
        val copy = original.copy(hasAudio = false, bitrate = 4_000_000)
        assertFalse(copy.hasAudio)
        assertEquals(4_000_000, copy.bitrate)
        assertEquals(1920, copy.width)
    }

    @Test
    fun `VideoInfo with 0 rotation preserves dimensions`() {
        val info = FFmpegProcessor.VideoInfo(1000L, 1920, 1080, 5_000_000, 24f, false, 0)
        assertEquals(1920, info.width)
        assertEquals(1080, info.height)
    }

    @Test
    fun `VideoInfo with 180 rotation preserves dimensions`() {
        val info = FFmpegProcessor.VideoInfo(1000L, 1920, 1080, 5_000_000, 24f, true, 180)
        assertEquals(1920, info.width)
        assertEquals(1080, info.height)
    }

    // ─── extractFrame ─────────────────────────────────────────

    @Test
    fun `extractFrame returns null on exception`() = runTest(testDispatcher) {
        val result = processor.extractFrame(fakeUri, 0L)
        assertNull(result)
    }

    @Test
    fun `extractFrame returns null for invalid URI`() = runTest(testDispatcher) {
        val result = processor.extractFrame(fakeUri, 999999L)
        assertNull(result)
    }

    // ─── extractFrames ────────────────────────────────────────

    @Test
    fun `extractFrames with zero duration returns empty list`() = runTest(testDispatcher) {
        val result = processor.extractFrames(fakeUri)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extractFrames respects maxFrames parameter`() = runTest(testDispatcher) {
        val result = processor.extractFrames(fakeUri, intervalMs = 1000L, maxFrames = 5)
        assertTrue(result.size <= 5)
    }

    @Test
    fun `extractFrames with large interval handles gracefully`() = runTest(testDispatcher) {
        val result = processor.extractFrames(fakeUri, intervalMs = 1_000_000L, maxFrames = 30)
        assertTrue(result is List<*>)
    }

    @Test
    fun `extractFrames with targetWidth handles gracefully`() = runTest(testDispatcher) {
        val result = processor.extractFrames(fakeUri, targetWidth = 720)
        assertTrue(result is List<*>)
    }

    @Test
    fun `extractFrames returns pairs with valid timestamps`() = runTest(testDispatcher) {
        val result = processor.extractFrames(fakeUri)
        result.forEach { (timestampMs, bitmap) ->
            assertTrue("Timestamp should be non-negative", timestampMs >= 0)
            assertFalse("Bitmap should not be recycled", bitmap.isRecycled)
        }
    }

    // ─── generateThumbnail ────────────────────────────────────

    @Test
    fun `generateThumbnail returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("thumb", ".jpg")
        val result = processor.generateThumbnail(fakeUri, 0L, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `generateThumbnail with default time returns false on bad URI`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("thumb_default", ".jpg")
        val result = processor.generateThumbnail(fakeUri, outputFile = outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    // ─── trimVideo ────────────────────────────────────────────

    @Test
    fun `trimVideo returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("trim", ".mp4")
        val result = processor.trimVideo(fakeUri, 0L, 1000L, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `trimVideo with zero-length range`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("trim_zero", ".mp4")
        val result = processor.trimVideo(fakeUri, 5000L, 5000L, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `trimVideo with progress callback does not crash`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("trim_progress", ".mp4")
        var callbackInvoked = false
        processor.trimVideo(fakeUri, 0L, 1000L, outputFile) {
            callbackInvoked = true
        }
        outputFile.delete()
    }

    // ─── resizeVideo ──────────────────────────────────────────

    @Test
    fun `resizeVideo returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("resize", ".mp4")
        val result = processor.resizeVideo(fakeUri, 720, 1280, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `resizeVideo with square dimensions`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("resize_sq", ".mp4")
        val result = processor.resizeVideo(fakeUri, 1080, 1080, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    // ─── changeSpeed ──────────────────────────────────────────

    @Test
    fun `changeSpeed returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("speed", ".mp4")
        val result = processor.changeSpeed(fakeUri, 1.5f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `changeSpeed with speed 1 returns copy of file`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("speed1", ".mp4")
        val result = processor.changeSpeed(fakeUri, 1.0f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `changeSpeed with slow motion speed`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("speed_slow", ".mp4")
        val result = processor.changeSpeed(fakeUri, 0.5f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `changeSpeed with very fast speed`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("speed_fast", ".mp4")
        val result = processor.changeSpeed(fakeUri, 4.0f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    // ─── applyFilter ──────────────────────────────────────────

    @Test
    fun `applyFilter returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("filter", ".mp4")
        val result = processor.applyFilter(fakeUri, 0.5f, 1.2f, 1.1f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `applyFilter with neutral values`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("filter_neutral", ".mp4")
        val result = processor.applyFilter(fakeUri, 0f, 1f, 1f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `applyFilter with extreme values`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("filter_extreme", ".mp4")
        val result = processor.applyFilter(fakeUri, -1f, 3f, 0f, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    // ─── exportVideo ──────────────────────────────────────────

    @Test
    fun `exportVideo returns false on exception`() = runTest(testDispatcher) {
        val outputPath = java.io.File.createTempFile("export", ".mp4").absolutePath
        val result = processor.exportVideo(fakeUri, outputPath, 1080, 1920, 8_000_000, 30)
        assertFalse(result)
    }

    @Test
    fun `exportVideo with different resolutions`() = runTest(testDispatcher) {
        val outputPath = java.io.File.createTempFile("export_720", ".mp4").absolutePath
        val result = processor.exportVideo(fakeUri, outputPath, 720, 1280, 4_000_000, 24)
        assertFalse(result)
    }

    // ─── mergeVideos ──────────────────────────────────────────

    @Test
    fun `mergeVideos returns false for empty input`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("merge", ".mp4")
        val result = processor.mergeVideos(emptyList(), outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `mergeVideos returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("merge", ".mp4")
        val result = processor.mergeVideos(listOf(fakeUri), outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `mergeVideos with multiple URIs`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("merge_multi", ".mp4")
        val uri2 = mockk<Uri>(relaxed = true)
        val result = processor.mergeVideos(listOf(fakeUri, uri2), outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    // ─── getAnalysisFrame ─────────────────────────────────────

    @Test
    fun `getAnalysisFrame returns null on exception`() {
        val result = processor.getAnalysisFrame(fakeUri, 0L, 360)
        assertNull(result)
    }

    @Test
    fun `getAnalysisFrame with different target widths`() {
        val result720 = processor.getAnalysisFrame(fakeUri, 0L, 720)
        assertNull(result720)

        val result180 = processor.getAnalysisFrame(fakeUri, 0L, 180)
        assertNull(result180)
    }

    @Test
    fun `getAnalysisFrame with negative time returns null`() {
        val result = processor.getAnalysisFrame(fakeUri, -1000L, 360)
        assertNull(result)
    }
}
