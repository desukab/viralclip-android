package com.viralclip.app.ui.viewmodels

import android.net.Uri
import com.viralclip.app.core.ai.CaptionGenerator
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.CaptionRepository
import com.viralclip.app.domain.repository.ClipRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var clipRepository: ClipRepository
    private lateinit var captionRepository: CaptionRepository
    private lateinit var captionGenerator: CaptionGenerator

    private lateinit var viewModel: CaptionsViewModel

    private val sampleClip = Clip(
        id = 10L, projectId = 1L, name = "Test Clip",
        sourceVideoUri = "content://test",
        startTimeMs = 0L, endTimeMs = 30000L,
        viralityScore = 0.85f
    )

    @Before
    fun setup() {
        clipRepository = mockk(relaxed = true)
        captionRepository = mockk(relaxed = true)
        captionGenerator = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() = CaptionsViewModel(clipRepository, captionRepository, captionGenerator)

    @Test
    fun `initial state has defaults`() = runTest {
        viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertNull("Initial clip should be null", state.clip)
        assertTrue("Initial captions should be empty", state.captions.isEmpty())
        assertFalse("Should not be generating", state.isGenerating)
        assertEquals("Default preset should be DEFAULT", CaptionPreset.DEFAULT, state.selectedPreset)
        assertEquals("Default language should be en", "en", state.selectedLanguage)
        assertEquals(12, state.availableLanguages.size)
    }

    @Test
    fun `loadClip populates state from clip`() = runTest {
        val clipWithCaptions = sampleClip.copy(
            captions = listOf(
                CaptionSegment(text = "Hello", startTimeMs = 0L, endTimeMs = 2000L),
                CaptionSegment(text = "World", startTimeMs = 2000L, endTimeMs = 4000L)
            ),
            captionStyle = CaptionStyle(preset = CaptionPreset.BOLD_HIGHLIGHT)
        )
        every { clipRepository.getClipById(10L) } returns flowOf(clipWithCaptions)

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.clip)
        assertEquals(2, state.captions.size)
        assertEquals(CaptionPreset.BOLD_HIGHLIGHT, state.selectedPreset)
    }

    @Test
    fun `updateCaptionPreset changes selected preset`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateCaptionPreset(CaptionPreset.KARAOKE)
        advanceUntilIdle()

        assertEquals(CaptionPreset.KARAOKE, viewModel.uiState.value.selectedPreset)
    }

    @Test
    fun `updateFontColor changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateFontColor(0xFFFF0000)
        advanceUntilIdle()

        assertEquals(0xFFFF0000, viewModel.uiState.value.currentCaptionStyle.fontColor)
    }

    @Test
    fun `updateHighlightColor changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateHighlightColor(0xFFFFFF00)
        advanceUntilIdle()

        assertEquals(0xFFFFFF00, viewModel.uiState.value.currentCaptionStyle.highlightColor)
    }

    @Test
    fun `updateFontSize changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateFontSize(48)
        advanceUntilIdle()

        assertEquals(48, viewModel.uiState.value.currentCaptionStyle.fontSize)
    }

    @Test
    fun `updatePosition changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updatePosition(CaptionPosition.TOP)
        advanceUntilIdle()

        assertEquals(CaptionPosition.TOP, viewModel.uiState.value.currentCaptionStyle.position)
    }

    @Test
    fun `updateAnimation changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateAnimation(CaptionAnimation.SCALE)
        advanceUntilIdle()

        assertEquals(CaptionAnimation.SCALE, viewModel.uiState.value.currentCaptionStyle.animation)
    }

    @Test
    fun `updateOutlineWidth changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateOutlineWidth(5f)
        advanceUntilIdle()

        assertEquals(5f, viewModel.uiState.value.currentCaptionStyle.outlineWidth, 0.001f)
    }

    @Test
    fun `updateOutlineColor changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateOutlineColor(0xFFFF0000)
        advanceUntilIdle()

        assertEquals(0xFFFF0000, viewModel.uiState.value.currentCaptionStyle.outlineColor)
    }

    @Test
    fun `updateCaseStyle changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateCaseStyle(CaseStyle.UPPERCASE)
        advanceUntilIdle()

        assertEquals(CaseStyle.UPPERCASE, viewModel.uiState.value.currentCaptionStyle.caseStyle)
    }

    @Test
    fun `updateLanguage changes selected language`() = runTest {
        viewModel = createViewModel()
        viewModel.updateLanguage("es")
        assertEquals("es", viewModel.uiState.value.selectedLanguage)
    }

    @Test
    fun `updateAlignment changes style`() = runTest {
        every { clipRepository.getClipById(any()) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.updateAlignment(Alignment.LEFT)
        advanceUntilIdle()

        assertEquals(Alignment.LEFT, viewModel.uiState.value.currentCaptionStyle.alignment)
    }

    @Test
    fun `startEditingCaption sets editing state`() = runTest {
        viewModel = createViewModel()
        viewModel.startEditingCaption(5L, "Edit me")
        assertEquals(5L, viewModel.uiState.value.editingCaptionId)
        assertEquals("Edit me", viewModel.uiState.value.editText)
    }

    @Test
    fun `updateEditText changes edit text`() = runTest {
        viewModel = createViewModel()
        viewModel.startEditingCaption(5L, "Initial")
        viewModel.updateEditText("Updated text")
        assertEquals("Updated text", viewModel.uiState.value.editText)
    }

    @Test
    fun `cancelCaptionEdit clears editing state`() = runTest {
        viewModel = createViewModel()
        viewModel.startEditingCaption(5L, "Edit me")
        viewModel.cancelCaptionEdit()
        assertNull(viewModel.uiState.value.editingCaptionId)
        assertEquals("", viewModel.uiState.value.editText)
    }

    @Test
    fun `saveCaptionEdit requires valid editing state`() = runTest {
        viewModel = createViewModel()
        viewModel.saveCaptionEdit()
        coVerify(exactly = 0) { captionRepository.updateCaption(any()) }
    }

    @Test
    fun `saveCaptionEdit updates caption`() = runTest {
        val captions = listOf(
            CaptionSegment(id = 1L, clipId = 10L, text = "Original", startTimeMs = 0L, endTimeMs = 2000L)
        )
        val clipWithCaptions = sampleClip.copy(captions = captions)
        every { clipRepository.getClipById(10L) } returns flowOf(clipWithCaptions)
        coEvery { captionRepository.updateCaption(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.startEditingCaption(1L, "Updated text")
        viewModel.saveCaptionEdit()
        advanceUntilIdle()

        coVerify { captionRepository.updateCaption(match { it.text == "Updated text" }) }
        assertNull(viewModel.uiState.value.editingCaptionId)
    }

    @Test
    fun `generateCaptions does nothing without loaded clip`() = runTest {
        viewModel = createViewModel()
        viewModel.generateCaptions()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isGenerating)
    }

    @Test
    fun `generateCaptions processes audio and creates captions`() = runTest {
        every { clipRepository.getClipById(10L) } returns flowOf(sampleClip)
        coEvery { clipRepository.updateClip(any()) } returns Unit
        coEvery { captionRepository.insertCaptions(any()) } returns Unit

        val transcriptionResult = CaptionGenerator.TranscriptionResult(
            segments = listOf(
                CaptionSegment(text = "Generated caption 1", startTimeMs = 500L, endTimeMs = 2500L),
                CaptionSegment(text = "Generated caption 2", startTimeMs = 3000L, endTimeMs = 5000L)
            ),
            language = "en",
            totalWords = 6,
            durationMs = 30000L
        )
        coEvery { captionGenerator.generateCaptions(any(), any()) } returns transcriptionResult

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.generateCaptions()
        advanceUntilIdle()

        assertFalse("Should not be generating after completion", viewModel.uiState.value.isGenerating)
        assertEquals(2, viewModel.uiState.value.captions.size)
    }

    @Test
    fun `deleteCaption removes caption from state`() = runTest {
        val captions = listOf(
            CaptionSegment(id = 1L, clipId = 10L, text = "C1", startTimeMs = 0L, endTimeMs = 2000L),
            CaptionSegment(id = 2L, clipId = 10L, text = "C2", startTimeMs = 2000L, endTimeMs = 4000L)
        )
        val clipWithCaptions = sampleClip.copy(captions = captions)
        every { clipRepository.getClipById(10L) } returns flowOf(clipWithCaptions)
        coEvery { captionRepository.deleteCaptionsByClipId(any()) } returns Unit

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        viewModel.deleteCaption(1L)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.captions.size)
        assertEquals(2L, viewModel.uiState.value.captions[0].id)
    }

    @Test
    fun `availableLanguages list contains expected languages`() = runTest {
        viewModel = createViewModel()
        val languages = viewModel.uiState.value.availableLanguages

        assertTrue(languages.any { it.first == "en" && it.second == "English" })
        assertTrue(languages.any { it.first == "es" && it.second == "Spanish" })
        assertTrue(languages.any { it.first == "fr" && it.second == "French" })
        assertTrue(languages.any { it.first == "de" && it.second == "German" })
        assertTrue(languages.any { it.first == "ja" && it.second == "Japanese" })
    }

    @Test
    fun `previewText default is set`() = runTest {
        viewModel = createViewModel()
        assertEquals("Your amazing captions appear here", viewModel.uiState.value.previewText)
    }
}
