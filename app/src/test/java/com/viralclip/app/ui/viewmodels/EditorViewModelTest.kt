package com.viralclip.app.ui.viewmodels

import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.CaptionRepository
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.ProjectRepository
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
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK private lateinit var projectRepository: ProjectRepository
    @MockK private lateinit var clipRepository: ClipRepository
    @MockK private lateinit var captionRepository: CaptionRepository

    private lateinit var viewModel: EditorViewModel

    private val sampleClip = Clip(
        id = 10L, projectId = 1L, name = "Clip 1",
        sourceVideoUri = "content://media/1", startTimeMs = 0L, endTimeMs = 3000L,
        viralityScore = 0.85f, order = 0
    )

    private val sampleProject = Project(
        id = 1L, name = "Test Project", sourceVideoUri = "content://test",
        sourceVideoPath = "/tmp/test.mp4", durationMs = 10000L, status = "completed",
        createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = EditorViewModel(projectRepository, clipRepository, captionRepository)

    @Test
    fun `initial state has empty clips and no project`() = runTest {
        viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertTrue("Initial clips should be empty", state.clips.isEmpty())
        assertNull("Initial project should be null", state.project)
        assertFalse("Initially not processing", state.isProcessing)
        assertTrue("Initial undo stack should be empty", state.undoStack.isEmpty())
        assertTrue("Initial redo stack should be empty", state.redoStack.isEmpty())
    }

    @Test
    fun `loadProject populates state with clips`() = runTest {
        every { projectRepository.getProjectById(1L) } returns flowOf(sampleProject)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Should have 1 clip", 1, state.clips.size)
        assertNotNull("Project should be loaded", state.project)
        assertEquals("Test Project", state.project?.name)
    }

    @Test
    fun `selectClip updates selectedClipIndex`() = runTest {
        viewModel = createViewModel()

        viewModel.selectClip(2)
        assertEquals(2, viewModel.uiState.value.selectedClipIndex)

        viewModel.selectClip(-1)
        assertEquals(0, viewModel.uiState.value.selectedClipIndex)
    }

    @Test
    fun `selectTool updates selectedTool`() = runTest {
        viewModel = createViewModel()

        viewModel.selectTool(EditorTool.CAPTIONS)
        assertEquals(EditorTool.CAPTIONS, viewModel.uiState.value.selectedTool)

        viewModel.selectTool(EditorTool.SPEED)
        assertEquals(EditorTool.SPEED, viewModel.uiState.value.selectedTool)
    }

    @Test
    fun `togglePlayPause toggles isPlaying`() = runTest {
        viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.togglePlayPause()
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.togglePlayPause()
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `undo on empty stack does not crash`() = runTest {
        viewModel = createViewModel()
        viewModel.undo()
        assertTrue(viewModel.uiState.value.undoStack.isEmpty())
    }

    @Test
    fun `redo on empty stack does not crash`() = runTest {
        viewModel = createViewModel()
        viewModel.redo()
        assertTrue(viewModel.uiState.value.redoStack.isEmpty())
    }

    @Test
    fun `trimClip pushes action to undo stack`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.trimClip(10L, 1000L, 4000L)
        advanceUntilIdle()

        assertEquals("Undo stack should have 1 action", 1, viewModel.uiState.value.undoStack.size)
        assertTrue("Action should be TrimClip", viewModel.uiState.value.undoStack.first() is EditorAction.TrimClip)
        coVerify { clipRepository.updateClip(any()) }
    }

    @Test
    fun `undo after trim reverts clip`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.trimClip(10L, 1000L, 4000L)
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals("Redo stack should have 1 action", 1, viewModel.uiState.value.redoStack.size)
        assertTrue("Undo stack should be empty", viewModel.uiState.value.undoStack.isEmpty())
    }

    @Test
    fun `deleteClip pushes action to undo stack`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.deleteClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.deleteClip(10L)
        advanceUntilIdle()

        assertEquals("Undo stack should have 1 action", 1, viewModel.uiState.value.undoStack.size)
        assertTrue("Action should be DeleteClip", viewModel.uiState.value.undoStack.first() is EditorAction.DeleteClip)
    }

    @Test
    fun `changeSpeed pushes action to undo stack`() = runTest {
        val clip = sampleClip.copy(speed = 1.0f)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(clip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.changeSpeed(10L, 1.5f)
        advanceUntilIdle()

        assertEquals("Undo stack should have 1 action", 1, viewModel.uiState.value.undoStack.size)
        val action = viewModel.uiState.value.undoStack.first() as EditorAction.ChangeSpeed
        assertEquals(1.0f, action.oldSpeed)
        assertEquals(1.5f, action.newSpeed)
    }

    @Test
    fun `updateCaptionStyle pushes action to undo stack`() = runTest {
        val newStyle = CaptionStyle(fontSize = 24f)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.updateCaptionStyle(10L, newStyle)
        advanceUntilIdle()

        assertEquals("Undo stack should have 1 action", 1, viewModel.uiState.value.undoStack.size)
        assertTrue("Action should be UpdateCaptionStyle", viewModel.uiState.value.undoStack.first() is EditorAction.UpdateCaptionStyle)
    }

    @Test
    fun `updateFilters pushes action to undo stack`() = runTest {
        val newFilters = ClipFilters(brightness = 1.2f)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.updateFilters(10L, newFilters)
        advanceUntilIdle()

        assertEquals("Undo stack should have 1 action", 1, viewModel.uiState.value.undoStack.size)
        assertTrue("Action should be UpdateFilter", viewModel.uiState.value.undoStack.first() is EditorAction.UpdateFilter)
    }

    @Test
    fun `undo then redo restores action`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.trimClip(10L, 500L, 2500L)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.undoStack.size)
        assertEquals(0, viewModel.uiState.value.redoStack.size)

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.undoStack.size)
        assertEquals(1, viewModel.uiState.value.redoStack.size)

        viewModel.redo()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.undoStack.size)
        assertEquals(0, viewModel.uiState.value.redoStack.size)
    }
}
