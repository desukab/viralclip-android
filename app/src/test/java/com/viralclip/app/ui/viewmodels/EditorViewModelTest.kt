package com.viralclip.app.ui.viewmodels

import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.CaptionRepository
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.ProjectRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var projectRepository: ProjectRepository
    private lateinit var clipRepository: ClipRepository
    private lateinit var captionRepository: CaptionRepository

    private lateinit var viewModel: EditorViewModel

    private val sampleClip = Clip(
        id = 10L, projectId = 1L, name = "Clip 1",
        sourceVideoUri = "content://media/1", startTimeMs = 0L, endTimeMs = 3000L,
        viralityScore = 0.85f, order = 0
    )

    private val sampleClip2 = Clip(
        id = 11L, projectId = 1L, name = "Clip 2",
        sourceVideoUri = "content://media/1", startTimeMs = 3000L, endTimeMs = 6000L,
        viralityScore = 0.75f, order = 1
    )

    private val sampleProject = Project(
        id = 1L, name = "Test Project", sourceVideoUri = "content://test",
        duration = 10000L,
        createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        projectRepository = mockk(relaxed = true)
        clipRepository = mockk(relaxed = true)
        captionRepository = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
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
        assertEquals("Default selected tool should be TRIM", EditorTool.TRIM, state.selectedTool)
        assertEquals("Initial position should be 0", 0L, state.currentPositionMs)
        assertFalse("Should not be playing", state.isPlaying)
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
    fun `loadProject updates state when project changes`() = runTest {
        val projectFlow = MutableStateFlow(sampleProject)
        val clipsFlow = MutableStateFlow(listOf(sampleClip))
        every { projectRepository.getProjectById(1L) } returns projectFlow
        every { clipRepository.getClipsByProjectId(1L) } returns clipsFlow

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        projectFlow.value = sampleProject.copy(name = "Updated")
        advanceUntilIdle()

        assertEquals("Updated", viewModel.uiState.value.project?.name)
    }

    @Test
    fun `selectClip updates selectedClipIndex`() = runTest {
        viewModel = createViewModel()

        viewModel.selectClip(2)
        assertEquals(0, viewModel.uiState.value.selectedClipIndex)

        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip, sampleClip2))
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.selectClip(0)
        assertEquals(0, viewModel.uiState.value.selectedClipIndex)

        viewModel.selectClip(1)
        assertEquals(1, viewModel.uiState.value.selectedClipIndex)

        viewModel.selectClip(-1)
        assertEquals(0, viewModel.uiState.value.selectedClipIndex)

        viewModel.selectClip(100)
        assertEquals(1, viewModel.uiState.value.selectedClipIndex)
    }

    @Test
    fun `selectTool updates selectedTool`() = runTest {
        viewModel = createViewModel()

        viewModel.selectTool(EditorTool.CAPTIONS)
        assertEquals(EditorTool.CAPTIONS, viewModel.uiState.value.selectedTool)

        viewModel.selectTool(EditorTool.SPEED)
        assertEquals(EditorTool.SPEED, viewModel.uiState.value.selectedTool)

        viewModel.selectTool(EditorTool.AUDIO)
        assertEquals(EditorTool.AUDIO, viewModel.uiState.value.selectedTool)

        viewModel.selectTool(EditorTool.ADJUST)
        assertEquals(EditorTool.ADJUST, viewModel.uiState.value.selectedTool)
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
    fun `updatePosition sets currentPositionMs`() = runTest {
        viewModel = createViewModel()
        viewModel.updatePosition(5000L)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)
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
    fun `trimClip for non-existent clip does nothing`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.trimClip(999L, 1000L, 4000L)
        advanceUntilIdle()

        assertTrue("Undo stack should be empty", viewModel.uiState.value.undoStack.isEmpty())
        coVerify(exactly = 0) { clipRepository.updateClip(any()) }
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
    fun `splitClip creates two clips from one`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.deleteClip(any()) } just Runs
        coEvery { clipRepository.insertClip(any()) } returns 100L

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.splitClip(10L, 1500L)
        advanceUntilIdle()

        coVerify { clipRepository.deleteClip(10L) }
        coVerify { clipRepository.insertClip(match { it.name == "Clip 1 (1)" && it.endTimeMs == 1500L }) }
        coVerify { clipRepository.insertClip(match { it.name == "Clip 1 (2)" && it.startTimeMs == 1500L }) }
        assertEquals(1, viewModel.uiState.value.undoStack.size)
        assertTrue(viewModel.uiState.value.undoStack.first() is EditorAction.SplitClip)
    }

    @Test
    fun `deleteClip pushes action to undo stack`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip, sampleClip2))
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
        assertEquals(1.0f, action.oldSpeed, 0.001f)
        assertEquals(1.5f, action.newSpeed, 0.001f)
    }

    @Test
    fun `updateCaptionStyle pushes action to undo stack`() = runTest {
        val newStyle = CaptionStyle(fontSize = 24, preset = CaptionPreset.BOLD_HIGHLIGHT)
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
        val newFilters = ClipFilters(brightness = 1.2f, contrast = 1.5f)
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

    @Test
    fun `redo applies trim action with new values`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.trimClip(10L, 500L, 2500L)
        advanceUntilIdle()
        viewModel.undo()
        advanceUntilIdle()

        viewModel.redo()
        advanceUntilIdle()

        coVerify(exactly = 2) { clipRepository.updateClip(any()) }
    }

    @Test
    fun `hasUnsavedChanges is set when actions are pushed`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.trimClip(10L, 1000L, 4000L)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `redoStack is cleared when new action is pushed`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.trimClip(10L, 500L, 2500L)
        advanceUntilIdle()
        viewModel.undo()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.redoStack.size)

        viewModel.trimClip(10L, 600L, 2600L)
        advanceUntilIdle()

        assertTrue("Redo stack should be cleared", viewModel.uiState.value.redoStack.isEmpty())
    }

    @Test
    fun `canUndo and canRedo reflect stack state`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        assertFalse("canUndo should be false initially", viewModel.uiState.value.canUndo)
        assertFalse("canRedo should be false initially", viewModel.uiState.value.canRedo)

        viewModel.trimClip(10L, 1000L, 4000L)
        advanceUntilIdle()
        assertTrue("canUndo should be true after action", viewModel.uiState.value.canUndo)

        viewModel.undo()
        advanceUntilIdle()
        assertTrue("canRedo should be true after undo", viewModel.uiState.value.canRedo)
    }

    @Test
    fun `reorderClips updates clip order`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip, sampleClip2))
        coEvery { clipRepository.updateClips(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.reorderClips(0, 1)
        advanceUntilIdle()

        coVerify { clipRepository.updateClips(any()) }
    }

    @Test
    fun `selectedClip returns clip at selectedClipIndex`() = runTest {
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip, sampleClip2))
        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.selectClip(1)
        assertEquals(sampleClip2.id, viewModel.uiState.value.selectedClip?.id)
    }

    @Test
    fun `selectedClip returns null when no clips`() = runTest {
        viewModel = createViewModel()
        assertNull(viewModel.uiState.value.selectedClip)
    }

    @Test
    fun `EditorAction sealed classes are all unique`() {
        val actions = listOf<EditorAction>(
            EditorAction.TrimClip(1L, 0L, 1000L, 500L, 1500L),
            EditorAction.UpdateCaptionStyle(1L, CaptionStyle(), CaptionStyle()),
            EditorAction.AddTextOverlay(TextOverlay(text = "t", startTimeMs = 0L, endTimeMs = 1000L)),
            EditorAction.RemoveTextOverlay(TextOverlay(text = "t", startTimeMs = 0L, endTimeMs = 1000L)),
            EditorAction.UpdateFilter(1L, ClipFilters(), ClipFilters()),
            EditorAction.ReorderClips(listOf(1L, 2L), listOf(2L, 1L)),
            EditorAction.DeleteClip(sampleClip, 0),
            EditorAction.SplitClip(1L, 500L),
            EditorAction.ChangeSpeed(1L, 1.0f, 2.0f)
        )
        assertEquals(9, actions.size)
    }

    @Test
    fun `undo speed change restores old speed`() = runTest {
        val clip = sampleClip.copy(speed = 1.0f)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(clip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.changeSpeed(10L, 2.0f)
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.redoStack.size)
    }

    @Test
    fun `undo caption style change restores old style`() = runTest {
        val newStyle = CaptionStyle(fontSize = 50)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.updateCaptionStyle(10L, newStyle)
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        coVerify(exactly = 2) { clipRepository.updateClip(any()) }
    }

    @Test
    fun `undo filter change restores old filters`() = runTest {
        val newFilters = ClipFilters(brightness = 1.0f)
        every { clipRepository.getClipsByProjectId(1L) } returns flowOf(listOf(sampleClip))
        coEvery { clipRepository.updateClip(any()) } just Runs

        viewModel = createViewModel()
        viewModel.loadProject(1L)
        advanceUntilIdle()

        viewModel.updateFilters(10L, newFilters)
        advanceUntilIdle()

        viewModel.undo()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.redoStack.size)
    }

    @Test
    fun `EditorUiState default tool is TRIM`() = runTest {
        viewModel = createViewModel()
        assertEquals(EditorTool.TRIM, viewModel.uiState.value.selectedTool)
    }
}
