package com.viralclip.app.data.repository

import com.viralclip.app.data.database.dao.*
import com.viralclip.app.data.database.entities.*
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val clipDao: ClipDao
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<Project>> =
        projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getProjectById(id: Long): Flow<Project?> =
        projectDao.getProjectById(id).map { it?.toDomain() }

    override fun getRecentProjects(limit: Int): Flow<List<Project>> =
        projectDao.getRecentProjects(limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertProject(project: Project): Long =
        projectDao.insert(ProjectEntity.fromDomain(project))

    override suspend fun updateProject(project: Project) =
        projectDao.update(ProjectEntity.fromDomain(project))

    override suspend fun deleteProject(id: Long) =
        projectDao.deleteById(id)

    /**
     * Duplicate a project and all its clips.
     * Creates a new project with "(Copy)" suffix and copies all clips.
     */
    override suspend fun duplicateProject(id: Long): Long {
        val originalProject = projectDao.getProjectById(id).first() ?: return -1
        val originalClips = clipDao.getClipsByProjectId(id).first()

        // Create duplicate project with new name
        val duplicateProject = originalProject.copy(
            id = 0, // Auto-generate new ID
            name = "${originalProject.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newProjectId = projectDao.insert(duplicateProject)

        // Copy all clips with new project ID
        val duplicateClips = originalClips.map { clip ->
            clip.copy(
                id = 0, // Auto-generate new ID
                projectId = newProjectId
            )
        }
        if (duplicateClips.isNotEmpty()) {
            clipDao.insertAll(duplicateClips)
        }

        return newProjectId
    }

    /**
     * Rename a project by ID. Uses .first() for single-emission Flow.
     */
    override suspend fun renameProject(id: Long, newName: String) {
        val project = projectDao.getProjectById(id).first()
        project?.let {
            projectDao.update(it.copy(name = newName, updatedAt = System.currentTimeMillis()))
        }
    }
}

@Singleton
class ClipRepositoryImpl @Inject constructor(
    private val clipDao: ClipDao
) : ClipRepository {

    override fun getClipsByProjectId(projectId: Long): Flow<List<Clip>> =
        clipDao.getClipsByProjectId(projectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getClipById(id: Long): Flow<Clip?> =
        clipDao.getClipById(id).map { it?.toDomain() }

    override suspend fun insertClip(clip: Clip): Long =
        clipDao.insert(ClipEntity.fromDomain(clip))

    override suspend fun insertClips(clips: List<Clip>) =
        clipDao.insertAll(clips.map { ClipEntity.fromDomain(it) })

    override suspend fun updateClip(clip: Clip) =
        clipDao.update(ClipEntity.fromDomain(clip))

    override suspend fun updateClips(clips: List<Clip>) =
        clipDao.updateAll(clips.map { ClipEntity.fromDomain(it) })

    override suspend fun deleteClip(id: Long) =
        clipDao.deleteById(id)

    /**
     * Reorder clips by updating their order field in a single batch.
     */
    override suspend fun reorderClips(clipIds: List<Long>) {
        clipIds.forEachIndexed { index, id ->
            clipDao.updateOrder(id, index)
        }
    }
}

@Singleton
class CaptionRepositoryImpl @Inject constructor(
    private val captionDao: CaptionDao
) : CaptionRepository {

    override fun getCaptionsByClipId(clipId: Long): Flow<List<CaptionSegment>> =
        captionDao.getCaptionsByClipId(clipId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertCaptions(captions: List<CaptionSegment>) =
        captionDao.insertAll(captions.map { CaptionEntity.fromDomain(it) })

    override suspend fun updateCaption(caption: CaptionSegment) =
        captionDao.update(CaptionEntity.fromDomain(caption))

    override suspend fun deleteCaptionsByClipId(clipId: Long) =
        captionDao.deleteByClipId(clipId)

    /**
     * Update caption style for a clip.
     * Style is stored on the Clip entity, not individual captions.
     * This method is a no-op since the ViewModel handles style updates via ClipRepository.
     */
    override suspend fun updateCaptionStyle(clipId: Long, style: CaptionStyle) {
        // Caption style is managed by ClipRepository.updateClip() in the ViewModel layer
    }
}

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<Template>> =
        templateDao.getAllTemplates().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTemplatesByCategory(category: TemplateCategory): Flow<List<Template>> =
        templateDao.getTemplatesByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTemplateById(id: Long): Flow<Template?> =
        templateDao.getTemplateById(id).map { it?.toDomain() }

    override suspend fun insertTemplate(template: Template): Long =
        templateDao.insert(TemplateEntity.fromDomain(template))

    override suspend fun deleteTemplate(id: Long) =
        templateDao.deleteCustom(id)
}

@Singleton
class BrandPresetRepositoryImpl @Inject constructor(
    private val brandPresetDao: BrandPresetDao
) : BrandPresetRepository {

    override fun getAllBrandPresets(): Flow<List<BrandPreset>> =
        brandPresetDao.getAllPresets().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getBrandPresetById(id: Long): Flow<BrandPreset?> =
        brandPresetDao.getPresetById(id).map { it?.toDomain() }

    override suspend fun insertBrandPreset(preset: BrandPreset): Long =
        brandPresetDao.insert(BrandPresetEntity.fromDomain(preset))

    override suspend fun updateBrandPreset(preset: BrandPreset) =
        brandPresetDao.update(BrandPresetEntity.fromDomain(preset))

    override suspend fun deleteBrandPreset(id: Long) =
        brandPresetDao.deleteById(id)
}
