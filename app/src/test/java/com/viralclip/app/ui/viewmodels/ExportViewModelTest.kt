package com.viralclip.app.ui.viewmodels

import com.viralclip.app.core.video.FFmpegProcessor
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.ClipRepository
import com.viralclip.app.domain.repository.ProjectRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    }

    @Test
    fun `selectQuality changes quality`() = runTest {
        viewModel = createViewModel()
        viewModel.selectQuality(ExportQuality.MEDIUM)
        assertEquals(ExportQuality.MEDIUM, viewModel.uiState.value.selectedQuality)
    }

    @Test
    fun `selectPlatform changes platform`() = runTest {
        viewModel = createViewModel()
        viewModel.selectPlatform(PlatformPreset.INSTAGRAM_REELS)
        assertEquals(PlatformPreset.INSTAGRAM_REELS, viewModel.uiState.value.selectedPlatform)
    }

    @Test
    fun `selectFps changes fps`() = runTest {
        viewModel = createViewModel()
        viewModel.selectFps(60)
        assertEquals(60, viewModel.uiState.value.selectedFps)
    }

    @Test
    fun `selectFormat changes format`() = runTest {
        viewModel = createViewModel()
        viewModel.selectFormat(VideoFormat.MOV)
        assertEquals(VideoFormat.MOV, viewModel.uiState.value.selectedFormat)
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
}
