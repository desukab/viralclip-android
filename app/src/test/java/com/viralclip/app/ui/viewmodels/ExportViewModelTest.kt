package com.viralclip.app.ui.viewmodels

import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.*
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
class ExportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var clipRepository: ClipRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var ffmpegProcessor: FFmpegProcessor

    private lateinit var viewModel: ExportViewModel

    private val sampleClip = Clip(
        id = 10L, projectId = 1L, name = "Test Clip",
        sourceVideoUri = "content://test",
        startTimeMs = 0L, endTimeMs = 30000L,
        viralityScore = 0.85f
    )

    private val sampleProject = Project(
        id = 1L, name = "Test Project",
        sourceVideoUri = "content://test",
        duration = 60000L
    )

    @Before
    fun setup() {
        clipRepository = mockk(relaxed = true)
        projectRepository = mockk(relaxed = true)
        ffmpegProcessor = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() = ExportViewModel(clipRepository, projectRepository, ffmpegProcessor)

    @Test
    fun `initial state has default export settings`() = runTest {
        viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertEquals("Default quality should be HIGH", ExportQuality.HIGH, state.selectedQuality)
        assertEquals("Default platform should be TIKTOK", PlatformPreset.TIKTOK, state.selectedPlatform)
        assertEquals("Default FPS should be 30", 30, state.selectedFps)
        assertEquals("Default format should be MP4", VideoFormat.MP4, state.selectedFormat)
        assertTrue("Default includeCaptions", state.includeCaptions)
        assertFalse("Initially not exporting", state.isExporting)
        assertEquals("Initial progress should be 0", 0f, state.exportProgress)
        assertFalse("Export not complete initially", state.exportComplete)
        assertNull("No export path initially", state.exportPath)
        assertNull("No error initially", state.errorMessage)
    }

    @Test
    fun `loadClip loads clip and project`() = runTest {
        every { clipRepository.getClipById(10L) } returns flowOf(sampleClip)
        every { projectRepository.getProjectById(1L) } returns flowOf(sampleProject)

        viewModel = createViewModel()
        viewModel.loadClip(10L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Clip should be loaded", state.clip)
        assertNotNull("Project should be loaded", state.project)
        assertEquals(10L, state.clip?.id)
        assertEquals(1L, state.project?.id)
    }

    @Test
    fun `loadClip with non-existent clip leaves state empty`() = runTest {
        every { clipRepository.getClipById(99L) } returns flowOf(null)

        viewModel = createViewModel()
        viewModel.loadClip(99L)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.clip)
    }

    @Test
    fun `selectQuality changes quality`() = runTest {
        viewModel = createViewModel()
        viewModel.selectQuality(ExportQuality.MEDIUM)
        assertEquals(ExportQuality.MEDIUM, viewModel.uiState.value.selectedQuality)

        viewModel.selectQuality(ExportQuality.ULTRA)
        assertEquals(ExportQuality.ULTRA, viewModel.uiState.value.selectedQuality)

        viewModel.selectQuality(ExportQuality.LOW)
        assertEquals(ExportQuality.LOW, viewModel.uiState.value.selectedQuality)
    }

    @Test
    fun `selectPlatform changes platform`() = runTest {
        viewModel = createViewModel()
        viewModel.selectPlatform(PlatformPreset.INSTAGRAM_REELS)
        assertEquals(PlatformPreset.INSTAGRAM_REELS, viewModel.uiState.value.selectedPlatform)

        viewModel.selectPlatform(PlatformPreset.YOUTUBE_SHORTS)
        assertEquals(PlatformPreset.YOUTUBE_SHORTS, viewModel.uiState.value.selectedPlatform)
    }

    @Test
    fun `selectFps changes fps`() = runTest {
        viewModel = createViewModel()
        viewModel.selectFps(60)
        assertEquals(60, viewModel.uiState.value.selectedFps)

        viewModel.selectFps(24)
        assertEquals(24, viewModel.uiState.value.selectedFps)
    }

    @Test
    fun `selectFormat changes format`() = runTest {
        viewModel = createViewModel()
        viewModel.selectFormat(VideoFormat.MOV)
        assertEquals(VideoFormat.MOV, viewModel.uiState.value.selectedFormat)

        viewModel.selectFormat(VideoFormat.WEBM)
        assertEquals(VideoFormat.WEBM, viewModel.uiState.value.selectedFormat)
    }

    @Test
    fun `toggleCaptions toggles includeCaptions`() = runTest {
        viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.includeCaptions)

        viewModel.toggleCaptions()
        assertFalse(viewModel.uiState.value.includeCaptions)

        viewModel.toggleCaptions()
        assertTrue(viewModel.uiState.value.includeCaptions)
    }

    @Test
    fun `dismissError clears error message`() = runTest {
        viewModel = createViewModel()
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `resetExport clears export state`() = runTest {
        viewModel = createViewModel()
        viewModel.resetExport()
        assertFalse(viewModel.uiState.value.exportComplete)
        assertNull(viewModel.uiState.value.exportPath)
    }

    @Test
    fun `exportWidth and exportHeight reflect platform`() = runTest {
        viewModel = createViewModel()
        viewModel.selectPlatform(PlatformPreset.TIKTOK)
        val state = viewModel.uiState.value
        assertEquals("Export width should match platform", state.selectedPlatform.width, state.exportWidth)
        assertEquals("Export height should match platform", state.selectedPlatform.height, state.exportHeight)
    }

    @Test
    fun `exportBitrate reflects quality`() = runTest {
        viewModel = createViewModel()
        viewModel.selectQuality(ExportQuality.LOW)
        assertEquals("Export bitrate should match quality", ExportQuality.LOW.bitrate, viewModel.uiState.value.exportBitrate)
    }

    @Test
    fun `exportDimensions change with platform selection`() = runTest {
        viewModel = createViewModel()

        viewModel.selectPlatform(PlatformPreset.INSTAGRAM_REELS)
        assertEquals(1080, viewModel.uiState.value.exportWidth)
        assertEquals(1920, viewModel.uiState.value.exportHeight)

        viewModel.selectPlatform(PlatformPreset.INSTAGRAM_FEED)
        assertEquals(1080, viewModel.uiState.value.exportWidth)
        assertEquals(1080, viewModel.uiState.value.exportHeight)

        viewModel.selectPlatform(PlatformPreset.TWITTER)
        assertEquals(1280, viewModel.uiState.value.exportWidth)
        assertEquals(720, viewModel.uiState.value.exportHeight)
    }

    @Test
    fun `exportBitrate scales with quality`() = runTest {
        viewModel = createViewModel()
        val original = viewModel.uiState.value.exportBitrate

        viewModel.selectQuality(ExportQuality.ULTRA)
        assertTrue(viewModel.uiState.value.exportBitrate > original)

        viewModel.selectQuality(ExportQuality.LOW)
        assertTrue(viewModel.uiState.value.exportBitrate < original)
    }

    @Test
    fun `selectPlatform updates state immediately`() = runTest {
        viewModel = createViewModel()
        val initial = viewModel.uiState.value.selectedPlatform

        viewModel.selectPlatform(PlatformPreset.FACEBOOK)
        assertNotEquals(initial, viewModel.uiState.value.selectedPlatform)
    }
}
