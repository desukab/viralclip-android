package com.viralclip.app.ui.viewmodels

import com.viralclip.app.domain.model.BrandPreset
import com.viralclip.app.domain.repository.BrandPresetRepository
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
class BrandViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var brandPresetRepository: BrandPresetRepository
    private lateinit var viewModel: BrandViewModel

    private val defaultPreset = BrandPreset(
        id = 1L, name = "My Brand",
        primaryColor = 0xFF7C3AED,
        secondaryColor = 0xFFEC4899,
        accentColor = 0xFF3B82F6
    )

    @Before
    fun setup() {
        brandPresetRepository = mockk(relaxed = true)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() = BrandViewModel(brandPresetRepository)

    @Test
    fun `initial state has empty presets`() = runTest {
        every { brandPresetRepository.getAllBrandPresets() } returns flowOf(emptyList())

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Initial presets should be empty", state.presets.isEmpty())
        assertFalse("Should not be loading initially", state.isLoading)
        assertNull("No error initially", state.errorMessage)
    }

    @Test
    fun `loadPresets populates presets list`() = runTest {
        val presets = listOf(
            defaultPreset,
            defaultPreset.copy(id = 2L, name = "Tech Brand"),
            defaultPreset.copy(id = 3L, name = "Gaming")
        )
        every { brandPresetRepository.getAllBrandPresets() } returns flowOf(presets)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.presets.size)
    }

    @Test
    fun `createPreset inserts new preset`() = runTest {
        every { brandPresetRepository.getAllBrandPresets() } returns flowOf(emptyList())
        coEvery { brandPresetRepository.insertBrandPreset(any()) } returns 5L

        viewModel = createViewModel()
        viewModel.createPreset("New Brand", 0xFFFF0000, 0xFF00FF00, 0xFF0000FF)
        advanceUntilIdle()

        coVerify {
            brandPresetRepository.insertBrandPreset(match {
                it.name == "New Brand" &&
                it.primaryColor == 0xFFFF0000 &&
                it.secondaryColor == 0xFF00FF00 &&
                it.accentColor == 0xFF0000FF
            })
        }
    }

    @Test
    fun `deletePreset removes preset by ID`() = runTest {
        every { brandPresetRepository.getAllBrandPresets() } returns flowOf(listOf(defaultPreset))
        coEvery { brandPresetRepository.deleteBrandPreset(any()) } just Runs

        viewModel = createViewModel()
        viewModel.deletePreset(1L)
        advanceUntilIdle()

        coVerify { brandPresetRepository.deleteBrandPreset(1L) }
    }

    @Test
    fun `seedDefaultPresets does nothing when presets exist`() = runTest {
        every { brandPresetRepository.getAllBrandPresets() } returns flowOf(listOf(defaultPreset))
        coEvery { brandPresetRepository.insertBrandPreset(any()) } returns 1L

        viewModel = createViewModel()
        viewModel.seedDefaultPresets()
        advanceUntilIdle()

        coVerify(exactly = 0) { brandPresetRepository.insertBrandPreset(any()) }
    }

    @Test
    fun `seedDefaultPresets creates defaults when empty`() = runTest {
        every { brandPresetRepository.getAllBrandPresets() } returns flowOf(emptyList()) andThen flowOf(any())
        coEvery { brandPresetRepository.insertBrandPreset(any()) } returns 1L

        viewModel = createViewModel()
        viewModel.seedDefaultPresets()
        advanceUntilIdle()

        coVerify(exactly = 3) { brandPresetRepository.insertBrandPreset(any()) }
    }

    @Test
    fun `BrandUiState data class works`() {
        val state = BrandUiState(
            presets = listOf(defaultPreset),
            isLoading = true,
            errorMessage = "Test error"
        )
        assertEquals(1, state.presets.size)
        assertTrue(state.isLoading)
        assertEquals("Test error", state.errorMessage)
    }

    @Test
    fun `BrandUiState default values are correct`() {
        val state = BrandUiState()
        assertTrue(state.presets.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `BrandPreset default values are correct`() {
        val preset = BrandPreset(name = "Test")
        assertEquals(0xFF7C3AED.toLong(), preset.primaryColor)
        assertEquals(0xFFEC4899.toLong(), preset.secondaryColor)
        assertEquals(0xFF3B82F6.toLong(), preset.accentColor)
        assertEquals("default", preset.fontFamily)
        assertFalse(preset.watermarkEnabled)
        assertNull(preset.logoPath)
    }

    @Test
    fun `BrandPreset equality works correctly`() {
        val preset1 = defaultPreset.copy(id = 1L)
        val preset2 = defaultPreset.copy(id = 1L)
        val preset3 = defaultPreset.copy(id = 2L)
        assertEquals(preset1, preset2)
        assertNotEquals(preset1, preset3)
    }
}
