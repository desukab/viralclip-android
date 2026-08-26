package com.viralclip.app.core.ai

import android.content.Context
import io.mockk.mockk
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
    private lateinit var generator: CaptionGenerator

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        generator = CaptionGenerator(context, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
}
