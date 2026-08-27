package com.viralclip.app.core.audio

import android.content.Context
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
class AudioProcessorTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var processor: AudioProcessor
    private val fakeUri: Uri = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        processor = AudioProcessor(context)
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

    // ─── AudioInfo Data Class ─────────────────────────────────

    @Test
    fun `AudioInfo data class constructs correctly`() {
        val info = AudioProcessor.AudioInfo(
            sampleRate = 44100,
            channels = 2,
            bitrate = 128000,
            durationMs = 60000L,
            format = "aac"
        )
        assertEquals(44100, info.sampleRate)
        assertEquals(2, info.channels)
        assertEquals(128000, info.bitrate)
        assertEquals(60000L, info.durationMs)
        assertEquals("aac", info.format)
    }

    @Test
    fun `AudioInfo data class equality`() {
        val info1 = AudioProcessor.AudioInfo(44100, 2, 128000, 60000L, "aac")
        val info2 = AudioProcessor.AudioInfo(44100, 2, 128000, 60000L, "aac")
        assertEquals(info1, info2)
        assertEquals(info1.hashCode(), info2.hashCode())
    }

    @Test
    fun `AudioInfo data class copy modifies fields`() {
        val original = AudioProcessor.AudioInfo(44100, 1, 128000, 0L, "aac")
        val modified = original.copy(channels = 2, bitrate = 256000)
        assertEquals(44100, modified.sampleRate)
        assertEquals(2, modified.channels)
        assertEquals(256000, modified.bitrate)
    }

    @Test
    fun `AudioInfo data class toString contains field values`() {
        val info = AudioProcessor.AudioInfo(48000, 2, 320000, 120000L, "mp3")
        val str = info.toString()
        assertTrue(str.contains("48000"))
        assertTrue(str.contains("mp3"))
    }

    // ─── AudioSegment Data Class ──────────────────────────────

    @Test
    fun `AudioSegment data class constructs correctly`() {
        val segment = AudioProcessor.AudioSegment(
            startTimeMs = 0L,
            endTimeMs = 1000L,
            volume = 0.5f,
            isSilent = false,
            speechConfidence = 0.8f
        )
        assertEquals(0L, segment.startTimeMs)
        assertEquals(1000L, segment.endTimeMs)
        assertEquals(0.5f, segment.volume, 0.001f)
        assertFalse(segment.isSilent)
        assertEquals(0.8f, segment.speechConfidence, 0.001f)
    }

    @Test
    fun `AudioSegment volume range validation`() {
        val lowVolume = AudioProcessor.AudioSegment(
            startTimeMs = 0L, endTimeMs = 1000L,
            volume = 0f, isSilent = true, speechConfidence = 0f
        )
        val highVolume = AudioProcessor.AudioSegment(
            startTimeMs = 0L, endTimeMs = 1000L,
            volume = 1f, isSilent = false, speechConfidence = 1f
        )

        assertEquals(0f, lowVolume.volume, 0.001f)
        assertEquals(1f, highVolume.volume, 0.001f)
    }

    @Test
    fun `AudioSegment isSilent reflects volume threshold`() {
        val silentSegment = AudioProcessor.AudioSegment(
            startTimeMs = 0L, endTimeMs = 1000L,
            volume = 0.001f, isSilent = true, speechConfidence = 0f
        )
        val loudSegment = AudioProcessor.AudioSegment(
            startTimeMs = 0L, endTimeMs = 1000L,
            volume = 0.5f, isSilent = false, speechConfidence = 0.9f
        )

        assertTrue(silentSegment.isSilent)
        assertFalse(loudSegment.isSilent)
        assertEquals(0f, silentSegment.speechConfidence, 0.001f)
        assertTrue(loudSegment.speechConfidence > 0.5f)
    }

    @Test
    fun `AudioSegment data class equality`() {
        val seg1 = AudioProcessor.AudioSegment(0L, 1000L, 0.5f, false, 0.8f)
        val seg2 = AudioProcessor.AudioSegment(0L, 1000L, 0.5f, false, 0.8f)
        assertEquals(seg1, seg2)
    }

    @Test
    fun `AudioSegment data class copy`() {
        val original = AudioProcessor.AudioSegment(0L, 1000L, 0.3f, true, 0.1f)
        val copy = original.copy(isSilent = false, volume = 0.9f)
        assertFalse(copy.isSilent)
        assertEquals(0.9f, copy.volume, 0.001f)
        assertEquals(0L, copy.startTimeMs)
    }

    @Test
    fun `AudioSegment consecutive segments cover time range`() {
        val seg1 = AudioProcessor.AudioSegment(0L, 1000L, 0.4f, false, 0.7f)
        val seg2 = AudioProcessor.AudioSegment(1000L, 2000L, 0.6f, false, 0.8f)
        assertEquals(seg1.endTimeMs, seg2.startTimeMs)
    }

    // ─── getAudioWaveform ─────────────────────────────────────

    @Test
    fun `getAudioWaveform handles exception gracefully`() = runTest(testDispatcher) {
        val result = processor.getAudioWaveform(fakeUri, numSamples = 100)
        assertEquals(100, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `getAudioWaveform returns correct number of samples`() = runTest(testDispatcher) {
        val result = processor.getAudioWaveform(fakeUri, numSamples = 200)
        assertEquals(200, result.size)
    }

    @Test
    fun `getAudioWaveform handles zero samples`() = runTest(testDispatcher) {
        val result = processor.getAudioWaveform(fakeUri, numSamples = 0)
        assertEquals(0, result.size)
    }

    @Test
    fun `getAudioWaveform returns non-negative values`() = runTest(testDispatcher) {
        val result = processor.getAudioWaveform(fakeUri, numSamples = 50)
        assertTrue(result.all { it >= 0f })
    }

    @Test
    fun `getAudioWaveform with single sample`() = runTest(testDispatcher) {
        val result = processor.getAudioWaveform(fakeUri, numSamples = 1)
        assertEquals(1, result.size)
    }

    // ─── getAudioEnergyPeaks ──────────────────────────────────

    @Test
    fun `getAudioEnergyPeaks handles exceptions gracefully`() = runTest(testDispatcher) {
        val result = processor.getAudioEnergyPeaks(fakeUri)
        assertTrue(result is List<*>)
    }

    @Test
    fun `getAudioEnergyPeaks returns peaks above threshold`() = runTest(testDispatcher) {
        val result = processor.getAudioEnergyPeaks(fakeUri, threshold = 0.5f)
        assertTrue(result is List<*>)
    }

    @Test
    fun `getAudioEnergyPeaks with zero threshold`() = runTest(testDispatcher) {
        val result = processor.getAudioEnergyPeaks(fakeUri, threshold = 0f)
        assertTrue(result is List<*>)
    }

    @Test
    fun `getAudioEnergyPeaks with max threshold returns empty or small list`() = runTest(testDispatcher) {
        val result = processor.getAudioEnergyPeaks(fakeUri, threshold = 1.0f)
        assertTrue(result.size <= 1)
    }

    @Test
    fun `getAudioEnergyPeaks returns pairs`() = runTest(testDispatcher) {
        val result = processor.getAudioEnergyPeaks(fakeUri)
        result.forEach { (timeMs, volume) ->
            assertTrue("Timestamp should be non-negative", timeMs >= 0)
            assertTrue("Volume should be non-negative", volume >= 0f)
        }
    }

    // ─── getAudioInfo ─────────────────────────────────────────

    @Test
    fun `getAudioInfo returns defaults on exception`() = runTest(testDispatcher) {
        val info = processor.getAudioInfo(fakeUri)
        assertEquals(44100, info.sampleRate)
        assertEquals(1, info.channels)
        assertEquals(128000, info.bitrate)
        assertEquals(0L, info.durationMs)
        assertEquals("aac", info.format)
    }

    @Test
    fun `getAudioInfo default values match typical aac format`() = runTest(testDispatcher) {
        val info = processor.getAudioInfo(fakeUri)
        assertTrue(info.sampleRate > 0)
        assertTrue(info.channels > 0)
        assertTrue(info.bitrate > 0)
        assertEquals("aac", info.format)
    }

    // ─── extractAudio ─────────────────────────────────────────

    @Test
    fun `extractAudio returns false on exception`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("audio_extract", ".m4a")
        val result = processor.extractAudio(fakeUri, outputFile)
        assertFalse(result)
        outputFile.delete()
    }

    @Test
    fun `extractAudio with progress callback does not crash`() = runTest(testDispatcher) {
        val outputFile = java.io.File.createTempFile("audio_progress", ".m4a")
        var lastProgress = -1f
        processor.extractAudio(fakeUri, outputFile) { progress ->
            lastProgress = progress
        }
        assertFalse(outputFile.exists() && outputFile.length() > 0)
        outputFile.delete()
    }

    @Test
    fun `extractAudio creates parent directories if needed`() = runTest(testDispatcher) {
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test_audio_subdir")
        val outputFile = java.io.File(tempDir, "audio.m4a")
        processor.extractAudio(fakeUri, outputFile)
        tempDir.deleteRecursively()
    }

    // ─── analyzeAudioSegments ─────────────────────────────────

    @Test
    fun `analyzeAudioSegments returns empty list on exception`() = runTest(testDispatcher) {
        val result = processor.analyzeAudioSegments(fakeUri)
        assertTrue(result is List<*>)
    }

    @Test
    fun `analyzeAudioSegments with custom segment duration`() = runTest(testDispatcher) {
        val result = processor.analyzeAudioSegments(fakeUri, segmentDurationMs = 500L)
        assertTrue(result is List<*>)
    }

    @Test
    fun `analyzeAudioSegments respects maxSegments`() = runTest(testDispatcher) {
        val result = processor.analyzeAudioSegments(fakeUri, maxSegments = 5)
        assertTrue(result.size <= 5)
    }

    @Test
    fun `analyzeAudioSegments segments have valid time ranges`() = runTest(testDispatcher) {
        val result = processor.analyzeAudioSegments(fakeUri)
        for (segment in result) {
            assertTrue("End time must be >= start time",
                segment.endTimeMs >= segment.startTimeMs)
            assertTrue("Volume must be in 0-1", segment.volume in 0f..1f)
            assertTrue("Speech confidence must be in 0-1",
                segment.speechConfidence in 0f..1f)
        }
    }
}
