package com.viralclip.app.ui.viewmodels

import com.viralclip.app.data.preferences.AppPreferences
import com.viralclip.app.data.preferences.UserPreferencesManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var prefsFlow: MutableStateFlow<AppPreferences>

    private lateinit var preferencesManager: UserPreferencesManager

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        preferencesManager = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
        prefsFlow = MutableStateFlow(AppPreferences())
        every { preferencesManager.preferences } returns prefsFlow
        coEvery { preferencesManager.updateDarkMode(any()) } just Runs
        coEvery { preferencesManager.updateGpuAcceleration(any()) } just Runs
        coEvery { preferencesManager.updateAutoSave(any()) } just Runs
        coEvery { preferencesManager.updateHapticFeedback(any()) } just Runs
        coEvery { preferencesManager.updateDefaultPlatform(any()) } just Runs
        coEvery { preferencesManager.updateDefaultQuality(any()) } just Runs
        coEvery { preferencesManager.updateDefaultFps(any()) } just Runs
        coEvery { preferencesManager.updateLanguage(any()) } just Runs
        coEvery { preferencesManager.incrementProcessedVideos() } just Runs
        coEvery { preferencesManager.incrementExportedClips() } just Runs
        coEvery { preferencesManager.clearCacheSize() } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() = SettingsViewModel(preferencesManager)

    @Test
    fun `initial state matches preferences defaults`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Dark mode should default to true", state.darkMode)
        assertTrue("GPU acceleration should default to true", state.gpuAcceleration)
        assertTrue("Auto-save should default to true", state.autoSave)
        assertTrue("Haptic feedback should default to true", state.hapticFeedback)
    }

    @Test
    fun `state updates when preferences change`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        prefsFlow.value = prefsFlow.value.copy(darkMode = false)
        advanceUntilIdle()

        assertFalse("Dark mode should be false after update", viewModel.uiState.value.darkMode)
    }

    @Test
    fun `updateDarkMode calls preferences manager`() = runTest {
        viewModel = createViewModel()
        viewModel.updateDarkMode(false)
        advanceUntilIdle()

        coVerify { preferencesManager.updateDarkMode(false) }
    }

    @Test
    fun `updateDarkMode with true calls preferences manager`() = runTest {
        viewModel = createViewModel()
        viewModel.updateDarkMode(true)
        advanceUntilIdle()

        coVerify { preferencesManager.updateDarkMode(true) }
    }

    @Test
    fun `updateGpuAcceleration calls preferences manager`() = runTest {
        viewModel = createViewModel()
        viewModel.updateGpuAcceleration(false)
        advanceUntilIdle()

        coVerify { preferencesManager.updateGpuAcceleration(false) }
    }

    @Test
    fun `updateAutoSave calls preferences manager`() = runTest {
        viewModel = createViewModel()
        viewModel.updateAutoSave(false)
        advanceUntilIdle()

        coVerify { preferencesManager.updateAutoSave(false) }
    }

    @Test
    fun `updateHapticFeedback calls preferences manager`() = runTest {
        viewModel = createViewModel()
        viewModel.updateHapticFeedback(false)
        advanceUntilIdle()

        coVerify { preferencesManager.updateHapticFeedback(false) }
    }

    @Test
    fun `clearCache calls preferences manager`() = runTest {
        viewModel = createViewModel()
        viewModel.clearCache()
        advanceUntilIdle()

        coVerify { preferencesManager.clearCacheSize() }
    }

    @Test
    fun `state reflects all preference fields`() = runTest {
        val customPrefs = AppPreferences(
            darkMode = false,
            gpuAcceleration = false,
            autoSave = false,
            hapticFeedback = false,
            defaultPlatform = "Instagram",
            defaultQuality = "Medium (720p)",
            defaultFps = 60,
            language = "es",
            cacheSizeMb = 256L,
            totalProcessedVideos = 42,
            totalExportedClips = 100
        )
        prefsFlow.value = customPrefs
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.darkMode)
        assertFalse(state.gpuAcceleration)
        assertFalse(state.autoSave)
        assertFalse(state.hapticFeedback)
        assertEquals("Instagram", state.defaultPlatform)
        assertEquals("Medium (720p)", state.defaultQuality)
        assertEquals(60, state.defaultFps)
        assertEquals("es", state.language)
        assertEquals(256L, state.cacheSizeMb)
        assertEquals(42, state.totalProcessedVideos)
        assertEquals(100, state.totalExportedClips)
    }

    @Test
    fun `preferences flow update reflects in state immediately`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        prefsFlow.value = prefsFlow.value.copy(
            defaultFps = 120,
            language = "fr"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(120, state.defaultFps)
        assertEquals("fr", state.language)
    }

    @Test
    fun `multiple sequential updates are handled correctly`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateDarkMode(false)
        viewModel.updateAutoSave(false)
        viewModel.updateGpuAcceleration(false)
        advanceUntilIdle()

        coVerify { preferencesManager.updateDarkMode(false) }
        coVerify { preferencesManager.updateAutoSave(false) }
        coVerify { preferencesManager.updateGpuAcceleration(false) }
    }

    @Test
    fun `initial cache size is zero`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0L, viewModel.uiState.value.cacheSizeMb)
    }

    @Test
    fun `initial fps is 30`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(30, viewModel.uiState.value.defaultFps)
    }
}
