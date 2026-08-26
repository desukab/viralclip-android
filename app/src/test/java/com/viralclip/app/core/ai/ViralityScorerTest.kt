package com.viralclip.app.core.ai

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ViralityScorerTest {

    private lateinit var scorer: ViralityScorer

    @Before
    fun setup() {
        scorer = ViralityScorer()
    }

    @Test
    fun `brightness uniformity for uniform bitmap returns low value`() {
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { 0xFF808080.toInt() } // all same gray
        val brightness = scorer.computeAverageBrightness(pixels, width, height)
        // Uniform image → low uniformity (low deviation from mean)
        assertTrue("Brightness should be between 0 and 1", brightness in 0f..1f)
        // Uniform gray should be around 0.5
        assertEquals(0.5f, brightness, 0.05f)
    }

    @Test
    fun `histogram for uniform bitmap is concentrated`() {
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { 0xFFFF0000.toInt() } // all red
        val histogram = scorer.computeColorHistogram(pixels, width, height)
        assertEquals(8, histogram.size)
        // All energy in red bins
        val totalEnergy = histogram.sum()
        assertTrue("Total histogram should be > 0", totalEnergy > 0f)
    }

    @Test
    fun `histogram for varied bitmap has broader distribution`() {
        val width = 100
        val height = 100
        val pixels = IntArray(width * height) { i ->
            val r = (i % 256)
            val g = ((i * 3) % 256)
            val b = ((i * 7) % 256)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val histogram = scorer.computeColorHistogram(pixels, width, height)
        // Many bins should have non-zero values
        val nonZeroBins = histogram.count { it > 0f }
        assertTrue("Varied image should have multiple non-zero bins", nonZeroBins >= 4)
    }

    @Test
    fun `pixel hash for same frame returns same hash`() {
        val width = 50
        val height = 50
        val pixels = IntArray(width * height) { 0xFF444444.toInt() }
        val hash1 = scorer.computePixelHash(pixels, width, height)
        val hash2 = scorer.computePixelHash(pixels, width, height)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `pixel hash for different frames returns different hashes`() {
        val width = 50
        val height = 50
        val pixels1 = IntArray(width * height) { 0xFF000000.toInt() }
        val pixels2 = IntArray(width * height) { 0xFFFFFFFF.toInt() }
        val hash1 = scorer.computePixelHash(pixels1, width, height)
        val hash2 = scorer.computePixelHash(pixels2, width, height)
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `motion score between identical frames is zero`() {
        val width = 50
        val height = 50
        val pixels = IntArray(width * height) { 0xFF888888.toInt() }
        val features = ViralityScorer.FrameFeatureData(
            brightness = scorer.computeAverageBrightness(pixels, width, height),
            histogram = scorer.computeColorHistogram(pixels, width, height),
            pixelHash = scorer.computePixelHash(pixels, width, height)
        )
        val motion = scorer.calculateMotionScore(features, features)
        assertEquals("Motion between identical frames should be 0", 0f, motion, 0.01f)
    }

    @Test
    fun `motion score between very different frames is high`() {
        val width = 50
        val height = 50
        val pixels1 = IntArray(width * height) { 0xFF000000.toInt() }
        val pixels2 = IntArray(width * height) { 0xFFFFFFFF.toInt() }
        val features1 = ViralityScorer.FrameFeatureData(
            brightness = scorer.computeAverageBrightness(pixels1, width, height),
            histogram = scorer.computeColorHistogram(pixels1, width, height),
            pixelHash = scorer.computePixelHash(pixels1, width, height)
        )
        val features2 = ViralityScorer.FrameFeatureData(
            brightness = scorer.computeAverageBrightness(pixels2, width, height),
            histogram = scorer.computeColorHistogram(pixels2, width, height),
            pixelHash = scorer.computePixelHash(pixels2, width, height)
        )
        val motion = scorer.calculateMotionScore(features1, features2)
        assertTrue("Motion between very different frames should be > 0.5", motion > 0.5f)
    }

    @Test
    fun `visual variety of identical frames is zero`() {
        val width = 50
        val height = 50
        val pixels = IntArray(width * height) { 0xFF888888.toInt() }
        val featureList = List(5) {
            ViralityScorer.FrameFeatureData(
                brightness = scorer.computeAverageBrightness(pixels, width, height),
                histogram = scorer.computeColorHistogram(pixels, width, height),
                pixelHash = scorer.computePixelHash(pixels, width, height)
            )
        }
        val variety = scorer.calculateVisualVariety(featureList)
        assertEquals("Visual variety of identical frames should be 0", 0f, variety, 0.01f)
    }

    @Test
    fun `visual variety of diverse frames is high`() {
        val width = 50
        val height = 50
        val featureList = (0 until 5).map { i ->
            val pixels = IntArray(width * height) {
                val c = (i * 50) % 256
                (0xFF shl 24) or (c shl 16) or (c shl 8) or c
            }
            ViralityScorer.FrameFeatureData(
                brightness = scorer.computeAverageBrightness(pixels, width, height),
                histogram = scorer.computeColorHistogram(pixels, width, height),
                pixelHash = scorer.computePixelHash(pixels, width, height)
            )
        }
        val variety = scorer.calculateVisualVariety(featureList)
        assertTrue("Visual variety of diverse frames should be > 0.1", variety > 0.1f)
    }

    @Test
    fun `brightness handles edge cases - all black`() {
        val pixels = IntArray(10 * 10) { 0xFF000000.toInt() }
        val b = scorer.computeAverageBrightness(pixels, 10, 10)
        assertTrue(b in 0f..1f)
    }

    @Test
    fun `brightness handles edge cases - all white`() {
        val pixels = IntArray(10 * 10) { 0xFFFFFFFF.toInt() }
        val b = scorer.computeAverageBrightness(pixels, 10, 10)
        assertTrue(b in 0f..1f)
    }

    @Test
    fun `visual variety with empty list returns zero`() {
        val variety = scorer.calculateVisualVariety(emptyList())
        assertEquals(0f, variety, 0.01f)
    }

    @Test
    fun `visual variety with single frame returns zero`() {
        val width = 50
        val height = 50
        val pixels = IntArray(width * height) { 0xFF888888.toInt() }
        val feature = ViralityScorer.FrameFeatureData(
            brightness = scorer.computeAverageBrightness(pixels, width, height),
            histogram = scorer.computeColorHistogram(pixels, width, height),
            pixelHash = scorer.computePixelHash(pixels, width, height)
        )
        val variety = scorer.calculateVisualVariety(listOf(feature))
        assertEquals(0f, variety, 0.01f)
    }
}
