package com.viralclip.app.data

import com.viralclip.app.data.database.dao.BrandPresetDao
import com.viralclip.app.data.database.dao.CaptionDao
import com.viralclip.app.data.database.dao.ClipDao
import com.viralclip.app.data.database.dao.ProjectDao
import com.viralclip.app.data.database.dao.TemplateDao
import com.viralclip.app.data.database.entities.BrandPresetEntity
import com.viralclip.app.data.database.entities.CaptionEntity
import com.viralclip.app.data.database.entities.ClipEntity
import com.viralclip.app.data.database.entities.ProjectEntity
import com.viralclip.app.data.database.entities.TemplateEntity
import com.viralclip.app.domain.model.BrandPreset
import com.viralclip.app.domain.model.CaptionSegment
import com.viralclip.app.domain.model.CaptionStyle
import com.viralclip.app.domain.model.Clip
import com.viralclip.app.domain.model.ClipFilters
import com.viralclip.app.domain.model.Project
import com.viralclip.app.domain.model.Template
import com.viralclip.app.domain.model.TemplateCategory
import com.viralclip.app.data.repository.BrandPresetRepositoryImpl
import com.viralclip.app.data.repository.CaptionRepositoryImpl
import com.viralclip.app.data.repository.ClipRepositoryImpl
import com.viralclip.app.data.repository.ProjectRepositoryImpl
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryTest {

    private lateinit var projectDao: ProjectDao
    private lateinit var clipDao: ClipDao
    private lateinit var captionDao: CaptionDao
    private lateinit var templateDao: TemplateDao
    private lateinit var brandPresetDao: BrandPresetDao

    @Before
    fun setup() {
        projectDao = mockk(relaxed = true)
        clipDao = mockk(relaxed = true)
        captionDao = mockk(relaxed = true)
        templateDao = mockk(relaxed = true)
        brandPresetDao = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─── ProjectRepository ─────────────────────────────────────────

    @Test
    fun `ProjectRepository getAllProjects maps entities to domain`() = runTest {
        val entity = ProjectEntity(id = 1L, name = "Test", sourceVideoUri = "uri")
        every { projectDao.getAllProjects() } returns flowOf(listOf(entity))

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        val projects = repo.getAllProjects().first()

        assertEquals(1, projects.size)
        assertEquals("Test", projects[0].name)
    }

    @Test
    fun `ProjectRepository getProjectById maps to domain`() = runTest {
        val entity = ProjectEntity(id = 5L, name = "P5", sourceVideoUri = "uri5")
        every { projectDao.getProjectById(5L) } returns flowOf(entity)

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        val project = repo.getProjectById(5L).first()

        assertNotNull(project)
        assertEquals(5L, project!!.id)
    }

    @Test
    fun `ProjectRepository getRecentProjects respects limit`() = runTest {
        val entities = (1..5).map {
            ProjectEntity(id = it.toLong(), name = "P$it", sourceVideoUri = "uri$it")
        }
        every { projectDao.getRecentProjects(5) } returns flowOf(entities)

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        val projects = repo.getRecentProjects(5).first()

        assertEquals(5, projects.size)
    }

    @Test
    fun `ProjectRepository insertProject delegates to DAO`() = runTest {
        val project = Project(name = "New", sourceVideoUri = "uri")
        every { projectDao.insert(any()) } returns 10L

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        val id = repo.insertProject(project)

        assertEquals(10L, id)
        coVerify { projectDao.insert(any()) }
    }

    @Test
    fun `ProjectRepository updateProject delegates to DAO`() = runTest {
        val project = Project(id = 1L, name = "Updated", sourceVideoUri = "uri")
        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        repo.updateProject(project)
        coVerify { projectDao.update(any()) }
    }

    @Test
    fun `ProjectRepository deleteProject delegates to DAO`() = runTest {
        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        repo.deleteProject(1L)
        coVerify { projectDao.deleteById(1L) }
    }

    @Test
    fun `ProjectRepository duplicateProject returns -1 for non-existent project`() = runTest {
        every { projectDao.getProjectById(99L) } returns flowOf(null)

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        val result = repo.duplicateProject(99L)

        assertEquals(-1L, result)
    }

    @Test
    fun `ProjectRepository duplicateProject creates copy with new ID`() = runTest {
        val original = ProjectEntity(
            id = 1L, name = "Original", sourceVideoUri = "uri",
            createdAt = 1000L, updatedAt = 2000L
        )
        val clips = listOf(
            ClipEntity(id = 1L, projectId = 1L, name = "C1", sourceVideoUri = "uri",
                startTimeMs = 0L, endTimeMs = 1000L),
            ClipEntity(id = 2L, projectId = 1L, name = "C2", sourceVideoUri = "uri",
                startTimeMs = 1000L, endTimeMs = 2000L)
        )
        every { projectDao.getProjectById(1L) } returns flowOf(original)
        every { clipDao.getClipsByProjectId(1L) } returns flowOf(clips)
        every { projectDao.insert(any()) } returns 2L

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        val newId = repo.duplicateProject(1L)

        assertEquals(2L, newId)
        coVerify { projectDao.insert(match { it.id == 0L && it.name == "Original (Copy)" }) }
        coVerify { clipDao.insertAll(match { list -> list.size == 2 && list.all { it.id == 0L && it.projectId == 2L } }) }
    }

    @Test
    fun `ProjectRepository renameProject updates name and timestamp`() = runTest {
        val project = ProjectEntity(
            id = 1L, name = "Old", sourceVideoUri = "uri",
            updatedAt = 1000L
        )
        every { projectDao.getProjectById(1L) } returns flowOf(project)

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        repo.renameProject(1L, "New")

        coVerify {
            projectDao.update(match {
                it.name == "New" && it.updatedAt > 1000L
            })
        }
    }

    @Test
    fun `ProjectRepository renameProject no-op for non-existent project`() = runTest {
        every { projectDao.getProjectById(99L) } returns flowOf(null)

        val repo = ProjectRepositoryImpl(projectDao, clipDao)
        repo.renameProject(99L, "New")

        coVerify(exactly = 0) { projectDao.update(any()) }
    }

    // ─── ClipRepository ─────────────────────────────────────────

    @Test
    fun `ClipRepository getClipsByProjectId maps to domain`() = runTest {
        val entity = ClipEntity(
            id = 1L, projectId = 10L, name = "C1",
            sourceVideoUri = "uri", startTimeMs = 0L, endTimeMs = 1000L
        )
        every { clipDao.getClipsByProjectId(10L) } returns flowOf(listOf(entity))

        val repo = ClipRepositoryImpl(clipDao)
        val clips = repo.getClipsByProjectId(10L).first()

        assertEquals(1, clips.size)
        assertEquals("C1", clips[0].name)
    }

    @Test
    fun `ClipRepository insertClip delegates to DAO`() = runTest {
        val clip = Clip(projectId = 1L, name = "Test", sourceVideoUri = "uri",
            startTimeMs = 0L, endTimeMs = 1000L)
        every { clipDao.insert(any()) } returns 5L

        val repo = ClipRepositoryImpl(clipDao)
        val id = repo.insertClip(clip)

        assertEquals(5L, id)
        coVerify { clipDao.insert(any()) }
    }

    @Test
    fun `ClipRepository insertClips delegates batch insertion`() = runTest {
        val clips = listOf(
            Clip(projectId = 1L, name = "C1", sourceVideoUri = "uri", startTimeMs = 0L, endTimeMs = 1000L),
            Clip(projectId = 1L, name = "C2", sourceVideoUri = "uri", startTimeMs = 1000L, endTimeMs = 2000L)
        )
        val repo = ClipRepositoryImpl(clipDao)
        repo.insertClips(clips)
        coVerify { clipDao.insertAll(match { it.size == 2 }) }
    }

    @Test
    fun `ClipRepository updateClips delegates batch update`() = runTest {
        val clips = listOf(
            Clip(id = 1L, projectId = 1L, name = "C1", sourceVideoUri = "uri", startTimeMs = 0L, endTimeMs = 1000L)
        )
        val repo = ClipRepositoryImpl(clipDao)
        repo.updateClips(clips)
        coVerify { clipDao.updateAll(any()) }
    }

    @Test
    fun `ClipRepository deleteClip delegates to DAO`() = runTest {
        val repo = ClipRepositoryImpl(clipDao)
        repo.deleteClip(5L)
        coVerify { clipDao.deleteById(5L) }
    }

    @Test
    fun `ClipRepository reorderClips updates orders correctly`() = runTest {
        val repo = ClipRepositoryImpl(clipDao)
        repo.reorderClips(listOf(3L, 1L, 2L))
        coVerify { clipDao.updateOrder(3L, 0) }
        coVerify { clipDao.updateOrder(1L, 1) }
        coVerify { clipDao.updateOrder(2L, 2) }
    }

    // ─── CaptionRepository ─────────────────────────────────────────

    @Test
    fun `CaptionRepository getCaptionsByClipId maps to domain`() = runTest {
        val entity = CaptionEntity(
            id = 1L, clipId = 5L, text = "Hello",
            startTimeMs = 0L, endTimeMs = 2000L
        )
        every { captionDao.getCaptionsByClipId(5L) } returns flowOf(listOf(entity))

        val repo = CaptionRepositoryImpl(captionDao)
        val captions = repo.getCaptionsByClipId(5L).first()

        assertEquals(1, captions.size)
        assertEquals("Hello", captions[0].text)
    }

    @Test
    fun `CaptionRepository insertCaptions delegates to DAO`() = runTest {
        val captions = listOf(
            CaptionSegment(clipId = 1L, text = "C1", startTimeMs = 0L, endTimeMs = 1000L)
        )
        val repo = CaptionRepositoryImpl(captionDao)
        repo.insertCaptions(captions)
        coVerify { captionDao.insertAll(any()) }
    }

    @Test
    fun `CaptionRepository updateCaption delegates to DAO`() = runTest {
        val caption = CaptionSegment(id = 1L, clipId = 1L, text = "Updated",
            startTimeMs = 0L, endTimeMs = 1000L)
        val repo = CaptionRepositoryImpl(captionDao)
        repo.updateCaption(caption)
        coVerify { captionDao.update(any()) }
    }

    @Test
    fun `CaptionRepository deleteCaptionsByClipId delegates to DAO`() = runTest {
        val repo = CaptionRepositoryImpl(captionDao)
        repo.deleteCaptionsByClipId(1L)
        coVerify { captionDao.deleteByClipId(1L) }
    }

    @Test
    fun `CaptionRepository updateCaptionStyle is no-op`() = runTest {
        val repo = CaptionRepositoryImpl(captionDao)
        repo.updateCaptionStyle(1L, CaptionStyle())
        coVerify(exactly = 0) { captionDao.update(any()) }
    }

    // ─── TemplateRepository ─────────────────────────────────────────

    @Test
    fun `TemplateRepository getAllTemplates maps to domain`() = runTest {
        val entity = TemplateEntity(
            id = 1L, name = "T1", category = TemplateCategory.VIRAL,
            captionStyle = CaptionStyle(), description = "desc"
        )
        every { templateDao.getAllTemplates() } returns flowOf(listOf(entity))

        val repo = com.viralclip.app.data.repository.TemplateRepositoryImpl(templateDao)
        val templates = repo.getAllTemplates().first()

        assertEquals(1, templates.size)
        assertEquals("T1", templates[0].name)
    }

    @Test
    fun `TemplateRepository getTemplatesByCategory maps to domain`() = runTest {
        val entity = TemplateEntity(
            id = 1L, name = "T1", category = TemplateCategory.VIRAL,
            captionStyle = CaptionStyle(), description = "desc"
        )
        every { templateDao.getTemplatesByCategory(TemplateCategory.VIRAL) } returns flowOf(listOf(entity))

        val repo = com.viralclip.app.data.repository.TemplateRepositoryImpl(templateDao)
        val templates = repo.getTemplatesByCategory(TemplateCategory.VIRAL).first()

        assertEquals(1, templates.size)
    }

    @Test
    fun `TemplateRepository insertTemplate delegates to DAO`() = runTest {
        val template = Template(
            name = "New", category = TemplateCategory.VIRAL,
            captionStyle = CaptionStyle(), description = "desc"
        )
        every { templateDao.insert(any()) } returns 7L

        val repo = com.viralclip.app.data.repository.TemplateRepositoryImpl(templateDao)
        val id = repo.insertTemplate(template)

        assertEquals(7L, id)
        coVerify { templateDao.insert(any()) }
    }

    @Test
    fun `TemplateRepository deleteTemplate delegates to DAO`() = runTest {
        val repo = com.viralclip.app.data.repository.TemplateRepositoryImpl(templateDao)
        repo.deleteTemplate(5L)
        coVerify { templateDao.deleteCustom(5L) }
    }

    // ─── BrandPresetRepository ─────────────────────────────────────────

    @Test
    fun `BrandPresetRepository getAllBrandPresets maps to domain`() = runTest {
        val entity = BrandPresetEntity(
            id = 1L, name = "Brand1",
            primaryColor = 0xFF0000FF, secondaryColor = 0xFF00FF00,
            accentColor = 0xFFFF0000
        )
        every { brandPresetDao.getAllPresets() } returns flowOf(listOf(entity))

        val repo = BrandPresetRepositoryImpl(brandPresetDao)
        val presets = repo.getAllBrandPresets().first()

        assertEquals(1, presets.size)
        assertEquals("Brand1", presets[0].name)
    }

    @Test
    fun `BrandPresetRepository insertBrandPreset delegates to DAO`() = runTest {
        val preset = BrandPreset(
            name = "New Brand", primaryColor = 0xFF0000FF,
            secondaryColor = 0xFF00FF00, accentColor = 0xFFFF0000
        )
        every { brandPresetDao.insert(any()) } returns 3L

        val repo = BrandPresetRepositoryImpl(brandPresetDao)
        val id = repo.insertBrandPreset(preset)

        assertEquals(3L, id)
        coVerify { brandPresetDao.insert(any()) }
    }

    @Test
    fun `BrandPresetRepository updateBrandPreset delegates to DAO`() = runTest {
        val preset = BrandPreset(
            id = 1L, name = "Updated",
            primaryColor = 0xFF0000FF, secondaryColor = 0xFF00FF00, accentColor = 0xFFFF0000
        )
        val repo = BrandPresetRepositoryImpl(brandPresetDao)
        repo.updateBrandPreset(preset)
        coVerify { brandPresetDao.update(any()) }
    }

    @Test
    fun `BrandPresetRepository deleteBrandPreset delegates to DAO`() = runTest {
        val repo = BrandPresetRepositoryImpl(brandPresetDao)
        repo.deleteBrandPreset(5L)
        coVerify { brandPresetDao.deleteById(5L) }
    }

    @Test
    fun `BrandPresetRepository getBrandPresetById maps to domain`() = runTest {
        val entity = BrandPresetEntity(
            id = 5L, name = "Brand5",
            primaryColor = 0xFF0000FF, secondaryColor = 0xFF00FF00, accentColor = 0xFFFF0000
        )
        every { brandPresetDao.getPresetById(5L) } returns flowOf(entity)

        val repo = BrandPresetRepositoryImpl(brandPresetDao)
        val preset = repo.getBrandPresetById(5L).first()

        assertNotNull(preset)
        assertEquals(5L, preset!!.id)
    }
}
