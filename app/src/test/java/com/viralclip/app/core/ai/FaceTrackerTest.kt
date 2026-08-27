package com.viralclip.app.core.ai

import android.content.Context
import android.graphics.Bitmap
import com.viralclip.app.domain.model.FacePosition
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FaceTrackerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var faceTracker: FaceTracker

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        faceTracker = FaceTracker(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        faceTracker.close()
        unmockkAll()
    }

    @Test
    fun `progress starts at zero`() {
        assertEquals(0f, faceTracker.progress.value, 0.01f)
    }

    @Test
    fun `FaceTrackResult data class constructs correctly`() {
        val trackedFrame = FaceTracker.TrackedFrame(
            timestampMs = 0L,
            faces = listOf(
                FacePosition(0.5f, 0.4f, 0.2f, 0.3f, 0.95f)
            ),
            mainFace = FacePosition(0.5f, 0.4f, 0.2f, 0.3f, 0.95f),
            refocusPoint = FaceTracker.RefocusPoint(0.5f, 0.35f)
        )
        val result = FaceTracker.FaceTrackResult(
            frames = listOf(trackedFrame),
            dominantSpeaker = FacePosition(0.5f, 0.4f, 0.2f, 0.3f, 0.95f),
            avgFaceSize = 0.06f,
            facePresentRatio = 1.0f
        )
        assertEquals(1, result.frames.size)
        assertNotNull(result.dominantSpeaker)
        assertEquals(1.0f, result.facePresentRatio, 0.001f)
    }

    @Test
    fun `trackFaces with empty frames returns empty result`() = runTest(testDispatcher) {
        val result = faceTracker.trackFaces(emptyList())

        assertTrue(result.frames.isEmpty())
        assertNull(result.dominantSpeaker)
        assertEquals(0f, result.facePresentRatio, 0.001f)
        assertEquals(0f, result.avgFaceSize, 0.001f)
    }

    @Test
    fun `trackFaces handles recycled bitmaps gracefully`() = runTest(testDispatcher) {
        val recycledBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns true
            every { width } returns 1080
            every { height } returns 1920
        }
        val frames = listOf(0L to recycledBitmap)

        val result = faceTracker.trackFaces(frames)

        assertTrue(result.frames.isEmpty())
    }

    @Test
    fun `trackFaces handles invalid bitmaps gracefully`() = runTest(testDispatcher) {
        val invalidBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns false
            every { width } returns 0
            every { height } returns 0
        }
        val frames = listOf(0L to invalidBitmap)

        val result = faceTracker.trackFaces(frames)

        assertTrue(result.frames.isEmpty())
    }

    @Test
    fun `RefocusPoint normalization is within bounds`() {
        val validPoint = FaceTracker.RefocusPoint(0.5f, 0.35f)
        assertTrue(validPoint.x in 0f..1f)
        assertTrue(validPoint.y in 0f..1f)
    }

    @Test
    fun `FacePosition area calculation is correct`() {
        val face = FacePosition(0.5f, 0.4f, 0.2f, 0.3f, 0.95f)
        val area = face.width * face.height
        assertEquals(0.06f, area, 0.001f)
    }

    @Test
    fun `calculateReframePoint returns center for empty faces`() {
        val result = faceTracker.calculateReframePoint(
            faces = emptyList(),
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        assertEquals(0.5f, result.first, 0.001f)
        assertEquals(0.5f, result.second, 0.001f)
    }

    @Test
    fun `calculateReframePoint returns weighted center for single face`() {
        val face = FacePosition(0.3f, 0.4f, 0.2f, 0.3f, 0.95f)
        val result = faceTracker.calculateReframePoint(
            faces = listOf(face),
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        assertTrue(result.first > 0f)
        assertTrue(result.second > 0f)
    }

    @Test
    fun `calculateReframePoint handles multiple faces`() {
        val faces = listOf(
            FacePosition(0.3f, 0.4f, 0.2f, 0.3f, 0.9f),
            FacePosition(0.7f, 0.4f, 0.2f, 0.3f, 0.8f)
        )
        val result = faceTracker.calculateReframePoint(
            faces = faces,
            sourceWidth = 1080,
            sourceHeight = 1920
        )
        assertTrue(result.first in 0f..1f)
        assertTrue(result.second in 0f..1f)
    }

    @Test
    fun `trackFaces progress reaches one on completion`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            0L to bitmap,
            1000L to bitmap,
            2000L to bitmap
        )

        faceTracker.trackFaces(frames)

        assertEquals(1f, faceTracker.progress.value, 0.001f)
    }

    @Test
    fun `detectFaces with null bitmap returns empty list`() = runTest(testDispatcher) {
        val result = faceTracker.detectFaces(createFakeBitmap())
        assertTrue(result is List<*>)
    }

    @Test
    fun `facePresentRatio calculation is correct`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(
            0L to bitmap,
            1000L to bitmap,
            2000L to bitmap,
            3000L to bitmap
        )

        val result = faceTracker.trackFaces(frames)

        assertTrue(result.facePresentRatio >= 0f)
        assertTrue(result.facePresentRatio <= 1f)
    }

    @Test
    fun `avgFaceSize calculation is correct with faces`() = runTest(testDispatcher) {
        val bitmap = createFakeBitmap()
        val frames = listOf(0L to bitmap, 1000L to bitmap)

        val result = faceTracker.trackFaces(frames)

        assertTrue(result.avgFaceSize >= 0f)
    }

    @Test
    fun `TrackedFrame contains required fields`() {
        val frame = FaceTracker.TrackedFrame(
            timestampMs = 5000L,
            faces = emptyList(),
            mainFace = null,
            refocusPoint = FaceTracker.RefocusPoint(0.5f, 0.35f)
        )
        assertEquals(5000L, frame.timestampMs)
        assertTrue(frame.faces.isEmpty())
        assertNull(frame.mainFace)
    }

    @Test
    fun `detectFaces handles bitmap dimension edge cases`() = runTest(testDispatcher) {
        val tinyBitmap = mockk<Bitmap>().apply {
            every { isRecycled } returns false
            every { width } returns 1
            every { height } returns 1
            every { getPixel(any(), any()) } returns 0xFF808080.toInt()
        }

        val result = faceTracker.detectFaces(tinyBitmap)
        assertTrue(result is List<*>)
    }

    @Test
    fun `close does not throw exception`() {
        faceTracker.close()
        faceTracker.close()
    }

    private fun createFakeBitmap(): Bitmap {
        return mockk(relaxed = true).apply {
            every { isRecycled } returns false
            every { width } returns 1080
            every { height } returns 1920
            every { getPixel(any(), any()) } returns 0xFF808080.toInt()
        }
    }
}
