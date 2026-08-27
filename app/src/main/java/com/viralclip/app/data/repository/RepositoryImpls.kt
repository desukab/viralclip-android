package com.viralclip.app.data.repository

import com.viralclip.app.data.database.dao.*
import com.viralclip.app.data.database.entities.*
import com.viralclip.app.domain.model.*
import com.viralclip.app.domain.repository.*
import com.viralclip.app.util.Result
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

    override suspend fun duplicateProject(id: Long): Long {
        val original = projectDao.getProjectByIdOnce(id)
            ?: return Result.failure(IllegalArgumentException("Project not found: $id")).let { -1L }
        val originalClips = clipDao.getClipsByProjectIdOnce(id)
        val duplicateProject = original.copy(
            id = 0,
            name = "${original.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newProjectId = projectDao.insert(duplicateProject)
        val duplicateClips = originalClips.map { it.copy(id = 0, projectId = newProjectId) }
        if (duplicateClips.isNotEmpty()) clipDao.insertAll(duplicateClips)
        return newProjectId
    }

    override suspend fun renameProject(id: Long, newName: String) {
        val project = projectDao.getProjectByIdOnce(id) ?: return
        projectDao.update(project.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun archiveProject(id: Long) {
        projectDao.setArchived(id, true)
    }

    suspend fun unarchiveProject(id: Long) {
        projectDao.setArchived(id, false)
    }

    suspend fun touchLastOpened(id: Long) {
        projectDao.touchLastOpened(id)
    }

    fun searchProjects(query: String): Flow<List<Project>> =
        projectDao.searchProjects(query).map { it.map { e -> e.toDomain() } }

    fun getArchivedProjects(): Flow<List<Project>> =
        projectDao.getArchivedProjects().map { it.map { e -> e.toDomain() } }
}

@Singleton
class ClipRepositoryImpl @Inject constructor(
    private val clipDao: ClipDao
) : ClipRepository {

    override fun getAllClips(): Flow<List<Clip>> =
        clipDao.getAllClips().map { it.map(ClipEntity::toDomain) }

    override fun getClipsByProjectId(projectId: Long): Flow<List<Clip>> =
        clipDao.getClipsByProjectId(projectId).map { it.map(ClipEntity::toDomain) }

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

    override suspend fun reorderClips(clipIds: List<Long>) {
        clipIds.forEachIndexed { index, id ->
            clipDao.updateOrder(id, index)
        }
    }

    fun getClipsByScore(projectId: Long): Flow<List<Clip>> =
        clipDao.getClipsByScore(projectId).map { it.map(ClipEntity::toDomain) }

    fun getSelectedClips(projectId: Long): Flow<List<Clip>> =
        clipDao.getSelectedClips(projectId).map { it.map(ClipEntity::toDomain) }

    fun getClipCount(projectId: Long): Flow<Int> = clipDao.getClipCount(projectId)

    suspend fun setClipSelected(clipId: Long, selected: Boolean) =
        clipDao.setSelected(clipId, selected)

    suspend fun markClipExported(clipId: Long, path: String) =
        clipDao.markExported(clipId, path)

    suspend fun updateExportProgress(clipId: Long, progress: Float) =
        clipDao.updateExportProgress(clipId, progress)

    suspend fun getMaxViralityScore(projectId: Long): Float =
        clipDao.getMaxViralityScore(projectId) ?: 0f
}

@Singleton
class CaptionRepositoryImpl @Inject constructor(
    private val captionDao: CaptionDao
) : CaptionRepository {

    override fun getCaptionsByClipId(clipId: Long): Flow<List<CaptionSegment>> =
        captionDao.getCaptionsByClipId(clipId).map { it.map(CaptionEntity::toDomain) }

    override suspend fun insertCaptions(captions: List<CaptionSegment>) =
        captionDao.insertAll(captions.map { CaptionEntity.fromDomain(it) })

    override suspend fun updateCaption(caption: CaptionSegment) =
        captionDao.update(CaptionEntity.fromDomain(caption))

    override suspend fun deleteCaptionsByClipId(clipId: Long) =
        captionDao.deleteByClipId(clipId)

    override suspend fun updateCaptionStyle(clipId: Long, style: CaptionStyle) {}

    suspend fun getCaptionAtTime(clipId: Long, timeMs: Long): CaptionSegment? =
        captionDao.getCaptionAtTime(clipId, timeMs)?.toDomain()

    suspend fun replaceCaptionsForClip(clipId: Long, captions: List<CaptionSegment>) {
        captionDao.replaceForClip(
            clipId,
            captions.map { CaptionEntity.fromDomain(it) }
        )
    }

    suspend fun getCountForClip(clipId: Long): Int =
        captionDao.getCountForClip(clipId)
}

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<Template>> =
        templateDao.getAllTemplates().map { it.map { e -> e.toDomain() } }

    override fun getTemplatesByCategory(category: TemplateCategory): Flow<List<Template>> =
        templateDao.getTemplatesByCategory(category).map { it.map { e -> e.toDomain() } }

    override fun getTemplateById(id: Long): Flow<Template?> =
        templateDao.getTemplateById(id).map { it?.toDomain() }

    override suspend fun insertTemplate(template: Template): Long =
        templateDao.insert(TemplateEntity.fromDomain(template))

    override suspend fun deleteTemplate(id: Long) =
        templateDao.deleteCustom(id)

    fun getTemplatesByUsage(): Flow<List<Template>> =
        templateDao.getAllTemplatesByUsage().map { it.map { e -> e.toDomain() } }

    fun getFreeTemplates(): Flow<List<Template>> =
        templateDao.getFreeTemplates().map { it.map { e -> e.toDomain() } }

    fun getPremiumTemplates(): Flow<List<Template>> =
        templateDao.getPremiumTemplates().map { it.map { e -> e.toDomain() } }

    fun searchTemplates(query: String): Flow<List<Template>> =
        templateDao.searchTemplates(query).map { it.map { e -> e.toDomain() } }

    suspend fun incrementUsage(id: Long) = templateDao.incrementUsage(id)

    suspend fun updateRating(id: Long, rating: Float) = templateDao.updateRating(id, rating)

    suspend fun getTemplateByIdOnce(id: Long): Template? =
        templateDao.getTemplateByIdOnce(id)?.toDomain()
}

@Singleton
class BrandPresetRepositoryImpl @Inject constructor(
    private val brandPresetDao: BrandPresetDao
) : BrandPresetRepository {

    override fun getAllBrandPresets(): Flow<List<BrandPreset>> =
        brandPresetDao.getAllPresets().map { it.map { e -> e.toDomain() } }

    override fun getBrandPresetById(id: Long): Flow<BrandPreset?> =
        brandPresetDao.getPresetById(id).map { it?.toDomain() }

    override suspend fun insertBrandPreset(preset: BrandPreset): Long =
        brandPresetDao.insert(BrandPresetEntity.fromDomain(preset))

    override suspend fun updateBrandPreset(preset: BrandPreset) =
        brandPresetDao.update(BrandPresetEntity.fromDomain(preset))

    override suspend fun deleteBrandPreset(id: Long) =
        brandPresetDao.deleteById(id)

    fun getRecentPresets(): Flow<List<BrandPreset>> =
        brandPresetDao.getAllPresetsByRecent().map { it.map { e -> e.toDomain() } }

    fun searchPresets(query: String): Flow<List<BrandPreset>> =
        brandPresetDao.searchPresets(query).map { it.map { e -> e.toDomain() } }

    suspend fun getPresetByIdOnce(id: Long): BrandPreset? =
        brandPresetDao.getPresetByIdOnce(id)?.toDomain()
}
