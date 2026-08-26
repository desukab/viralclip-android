package com.viralclip.app.ui.viewmodels

import com.viralclip.app.data.preferences.UserPreferences
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
    private lateinit var prefsFlow: MutableStateFlow<UserPreferences>

    @MockK private lateinit var preferencesManager: UserPreferencesManager

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        prefsFlow = MutableStateFlow(UserPreferences())
        every { preferencesManager.preferences } returns prefsFlow
        coEvery { preferencesManager.updateDarkMode(any()) } just Runs
        coEvery { preferencesManager.updateGpuAcceleration(any()) } just Runs
        coEvery { preferencesManager.updateAutoSave(any()) } just Runs
        coEvery { preferencesManager.updateHapticFeedback(any()) } just Runs
        coEvery { preferencesManager.clearCacheSize() } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        val customPrefs = UserPreferences(
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
}
