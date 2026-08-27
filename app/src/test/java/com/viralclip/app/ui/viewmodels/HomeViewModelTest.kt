package com.viralclip.app.ui.viewmodels

import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.Clip
import com.viralclip.app.domain.model.ProcessingState
import com.viralclip.app.domain.model.Project
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.ProjectRepository
import com.viralclip.app.services.VideoProcessingPipeline
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var projectRepository: ProjectRepository
    private lateinit var clipRepository: ClipRepository
    private lateinit var pipeline: VideoProcessingPipeline
    private lateinit var ffmpegProcessor: FFmpegProcessor

    private lateinit var pipelineStateFlow: MutableStateFlow<ProcessingState>

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        projectRepository = mockk(relaxed = true)
        clipRepository = mockk(relaxed = true)
        pipeline = mockk(relaxed = true)
        ffmpegProcessor = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
        pipelineStateFlow = MutableStateFlow(ProcessingState.Idle)
        every { projectRepository.getRecentProjects(any()) } returns flowOf(emptyList())
        every { clipRepository.getAllClips() } returns flowOf(emptyList())
        every { pipeline.state } returns pipelineStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() = HomeViewModel(projectRepository, clipRepository, pipeline, ffmpegProcessor)

    @Test
    fun `initial state has empty projects`() = runTest {
        viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertTrue("Initial projects should be empty", state.recentProjects.isEmpty())
        assertNull("No error initially", state.errorMessage)
        assertFalse("Import dialog hidden initially", state.showImportDialog)
        assertFalse("Delete confirmation hidden initially", state.showDeleteConfirmation == null)
    }

    @Test
    fun `loadProjects populates projects list`() = runTest {
        val projects = listOf(
            Project(id = 1L, name = "Project 1", sourceVideoUri = "c1",
                duration = 5000L, createdAt = 0L, updatedAt = 0L),
            Project(id = 2L, name = "Project 2", sourceVideoUri = "c2",
                duration = 10000L, createdAt = 0L, updatedAt = 0L)
        )
        every { projectRepository.getRecentProjects(50) } returns flowOf(projects)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Should have 2 projects", 2, state.recentProjects.size)
        assertEquals(2, state.totalVideosProcessed)
    }

    @Test
    fun `deleteProject calls repository`() = runTest {
        coEvery { projectRepository.deleteProject(1L) } returns Unit

        viewModel = createViewModel()
        viewModel.deleteProject(1L)
        advanceUntilIdle()

        coVerify { projectRepository.deleteProject(1L) }
    }

    @Test
    fun `dismissError clears error message`() = runTest {
        viewModel = createViewModel()
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `showImportDialog sets showImportDialog true`() = runTest {
        viewModel = createViewModel()
        viewModel.showImportDialog()
        assertTrue(viewModel.uiState.value.showImportDialog)
    }

    @Test
    fun `hideImportDialog sets showImportDialog false`() = runTest {
        viewModel = createViewModel()
        viewModel.showImportDialog()
        viewModel.hideImportDialog()
        assertFalse(viewModel.uiState.value.showImportDialog)
    }

    @Test
    fun `pipeline state is reflected in UI state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.Analyzing(progress = 0.5f)
        advanceUntilIdle()

        assertTrue("Should reflect analyzing state",
            viewModel.uiState.value.processingState is ProcessingState.Analyzing)
    }

    @Test
    fun `pipeline idle state is reflected`() = runTest {
        viewModel = createViewModel()
        pipelineStateFlow.value = ProcessingState.Idle
        advanceUntilIdle()

        assertTrue("Should reflect idle state",
            viewModel.uiState.value.processingState is ProcessingState.Idle)
    }

    @Test
    fun `pipeline complete state sets isProcessing to false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.Analyzing(progress = 0.5f)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProcessing)

        pipelineStateFlow.value = ProcessingState.Complete
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun `pipeline error state sets isProcessing to false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.Analyzing(progress = 0.5f)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProcessing)

        pipelineStateFlow.value = ProcessingState.Error("Something failed")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun `showDeleteConfirmation sets projectId`() = runTest {
        viewModel = createViewModel()
        viewModel.showDeleteConfirmation(42L)
        assertEquals(42L, viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `dismissDeleteConfirmation clears projectId`() = runTest {
        viewModel = createViewModel()
        viewModel.showDeleteConfirmation(42L)
        viewModel.dismissDeleteConfirmation()
        assertNull(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `dismissProcessing resets to idle`() = runTest {
        viewModel = createViewModel()
        pipelineStateFlow.value = ProcessingState.Analyzing(progress = 0.5f)
        advanceUntilIdle()

        viewModel.dismissProcessing()
        assertTrue(viewModel.uiState.value.processingState is ProcessingState.Idle)
    }

    @Test
    fun `pipeline transcribing state reflected`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.Transcribing(progress = 0.3f)
        advanceUntilIdle()

        assertTrue("Should reflect transcribing state",
            viewModel.uiState.value.processingState is ProcessingState.Transcribing)
    }

    @Test
    fun `pipeline detectingFaces state reflected`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.DetectingFaces(progress = 0.4f)
        advanceUntilIdle()

        assertTrue("Should reflect detecting faces state",
            viewModel.uiState.value.processingState is ProcessingState.DetectingFaces)
    }

    @Test
    fun `pipeline scoringVirality state reflected`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.ScoringVirality(progress = 0.6f)
        advanceUntilIdle()

        assertTrue("Should reflect scoring state",
            viewModel.uiState.value.processingState is ProcessingState.ScoringVirality)
    }

    @Test
    fun `pipeline generatingClips state reflected`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        pipelineStateFlow.value = ProcessingState.GeneratingClips(progress = 0.8f)
        advanceUntilIdle()

        assertTrue("Should reflect generating clips state",
            viewModel.uiState.value.processingState is ProcessingState.GeneratingClips)
    }

    @Test
    fun `totalClipsCreated updates with clip count`() = runTest {
        val clips = listOf(
            Clip(id = 1L, projectId = 1L, name = "C1", sourceVideoUri = "u", startTimeMs = 0L, endTimeMs = 1000L),
            Clip(id = 2L, projectId = 1L, name = "C2", sourceVideoUri = "u", startTimeMs = 0L, endTimeMs = 1000L),
            Clip(id = 3L, projectId = 1L, name = "C3", sourceVideoUri = "u", startTimeMs = 0L, endTimeMs = 1000L)
        )
        every { clipRepository.getAllClips() } returns flowOf(clips)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.totalClipsCreated)
    }
}
