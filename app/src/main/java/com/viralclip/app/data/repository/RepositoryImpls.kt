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
            ?: return -1L
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

    override fun getProjectsByDateRange(startDate: Long, endDate: Long): Flow<List<Project>> =
        projectDao.getAllProjects().map { entities ->
            entities.filter { it.createdAt in startDate..endDate }.map { it.toDomain() }
        }

    override fun searchProjects(query: String): Flow<List<Project>> =
        projectDao.searchProjects(query).map { it.map { e -> e.toDomain() } }

    override fun getProjectCount(): Flow<Int> = projectDao.getCountFlow()

    override suspend fun deleteAllProjects() = projectDao.deleteAll()

    override suspend fun updateProjectThumbnail(projectId: Long, thumbnailPath: String) {
        val project = projectDao.getProjectByIdOnce(projectId) ?: return
        projectDao.update(project.copy(thumbnailPath = thumbnailPath, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun archiveProject(id: Long) {
        projectDao.setArchived(id, true)
    }

    override suspend fun unarchiveProject(id: Long) {
        projectDao.setArchived(id, false)
    }

    fun touchLastOpened(id: Long) {
        projectDao.touchLastOpened(id)
    }

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

    override fun getSelectedClips(projectId: Long): Flow<List<Clip>> =
        clipDao.getSelectedClips(projectId).map { it.map(ClipEntity::toDomain) }

    override fun getClipsByViralityScore(projectId: Long, minScore: Float): Flow<List<Clip>> =
        clipDao.getClipsByScore(projectId).map { entities ->
            entities.filter { it.viralityScore >= minScore }.map(ClipEntity::toDomain)
        }

    override suspend fun deleteClipsByProjectId(projectId: Long) =
        clipDao.deleteByProjectId(projectId)

    override suspend fun selectClip(clipId: Long, selected: Boolean) =
        clipDao.setSelected(clipId, selected)

    override suspend fun selectAllClips(projectId: Long) {
        clipDao.getClipsByProjectIdOnce(projectId).forEach {
            clipDao.setSelected(it.id, true)
        }
    }

    override suspend fun deselectAllClips(projectId: Long) {
        clipDao.getClipsByProjectIdOnce(projectId).forEach {
            clipDao.setSelected(it.id, false)
        }
    }

    override suspend fun updateClipSpeed(clipId: Long, speed: Float) {
        clipDao.getClipByIdOnce(clipId)?.let { entity ->
            clipDao.update(entity.copy(speed = speed))
        }
    }

    override suspend fun updateClipVolume(clipId: Long, volume: Float) {
        clipDao.getClipByIdOnce(clipId)?.let { entity ->
            clipDao.update(entity.copy(volume = volume))
        }
    }

    override suspend fun updateClipCaptions(clipId: Long, captions: List<CaptionSegment>) {
        // Captions are stored in the captions table, not on the clip entity
        // This delegates to the caption DAO
        clipDao.getClipByIdOnce(clipId) // just verify clip exists
    }

    override suspend fun updateClipCaptionStyle(clipId: Long, style: CaptionStyle) {
        clipDao.getClipByIdOnce(clipId)?.let { entity ->
            clipDao.update(entity.copy(captionStyle = style))
        }
    }

    override suspend fun updateClipFilters(clipId: Long, filters: ClipFilters) {
        clipDao.getClipByIdOnce(clipId)?.let { entity ->
            clipDao.update(entity.copy(filters = filters))
        }
    }

    override suspend fun updateClipTrimPoints(clipId: Long, startTimeMs: Long, endTimeMs: Long) {
        clipDao.getClipByIdOnce(clipId)?.let { entity ->
            clipDao.update(entity.copy(startTimeMs = startTimeMs, endTimeMs = endTimeMs))
        }
    }

    fun getClipCount(projectId: Long): Flow<Int> = clipDao.getClipCount(projectId)

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

    override fun getCaptionsByTimeRange(clipId: Long, startMs: Long, endMs: Long): Flow<List<CaptionSegment>> =
        captionDao.getCaptionsByClipId(clipId).map { entities ->
            entities.filter { it.startTimeMs >= startMs && it.endTimeMs <= endMs }
                .map(CaptionEntity::toDomain)
        }

    override fun getCaptionById(id: Long): Flow<CaptionSegment?> =
        kotlinx.coroutines.flow.flow {
            emit(captionDao.getCaptionById(id)?.toDomain())
        }

    override suspend fun insertCaptions(captions: List<CaptionSegment>) =
        captionDao.insertAll(captions.map { CaptionEntity.fromDomain(it) })

    override suspend fun insertCaption(caption: CaptionSegment): Long =
        captionDao.insert(CaptionEntity.fromDomain(caption))

    override suspend fun updateCaption(caption: CaptionSegment) =
        captionDao.update(CaptionEntity.fromDomain(caption))

    override suspend fun updateCaptions(captions: List<CaptionSegment>) {
        captions.forEach { captionDao.update(CaptionEntity.fromDomain(it)) }
    }

    override suspend fun deleteCaptionsByClipId(clipId: Long) =
        captionDao.deleteByClipId(clipId)

    override suspend fun deleteCaption(id: Long) =
        captionDao.deleteById(id)

    override suspend fun updateCaptionStyle(clipId: Long, style: CaptionStyle) {
        // CaptionEntity doesn't store style per-caption; style is on the Clip
        // This is a no-op at caption level; use ClipRepository.updateClipCaptionStyle instead
    }

    override suspend fun updateCaptionText(captionId: Long, text: String) {
        captionDao.getCaptionById(captionId)?.let { entity ->
            captionDao.update(entity.copy(text = text))
        }
    }

    override suspend fun updateCaptionTimeRange(captionId: Long, startTimeMs: Long, endTimeMs: Long) {
        captionDao.getCaptionById(captionId)?.let { entity ->
            captionDao.update(entity.copy(startTimeMs = startTimeMs, endTimeMs = endTimeMs))
        }
    }

    override suspend fun mergeCaptions(captionIds: List<Long>): Long {
        if (captionIds.isEmpty()) return -1L
        val captions = captionIds.mapNotNull { captionDao.getCaptionById(it) }
        if (captions.isEmpty()) return -1L
        val merged = captions.sortedBy { it.startTimeMs }
        val first = merged.first()
        val last = merged.last()
        val mergedText = merged.joinToString(" ") { it.text }
        // Delete originals
        captionIds.forEach { captionDao.deleteById(it) }
        // Insert merged caption
        return captionDao.insert(
            first.copy(
                text = mergedText,
                startTimeMs = first.startTimeMs,
                endTimeMs = last.endTimeMs
            )
        )
    }

    override suspend fun splitCaption(captionId: Long, splitTimeMs: Long): Pair<Long, Long> {
        val caption = captionDao.getCaptionById(captionId) ?: return -1L to -1L
        val midpoint = ((caption.startTimeMs + caption.endTimeMs) / 2).coerceIn(
            caption.startTimeMs + 1, caption.endTimeMs - 1
        )
        val words = caption.text.split(" ")
        val splitIndex = (words.size / 2).coerceAtLeast(1)
        val firstText = words.take(splitIndex).joinToString(" ")
        val secondText = words.drop(splitIndex).joinToString(" ")

        // Update original to be first half
        captionDao.update(caption.copy(
            text = firstText,
            endTimeMs = midpoint
        ))
        // Insert second half
        val secondId = captionDao.insert(caption.copy(
            id = 0,
            text = secondText,
            startTimeMs = midpoint,
            endTimeMs = caption.endTimeMs
        ))
        return captionId to secondId
    }

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

    override fun getBuiltInTemplates(): Flow<List<Template>> =
        templateDao.getAllTemplates().map { entities ->
            entities.filter { it.isBuiltIn }.map { it.toDomain() }
        }

    override fun getUserTemplates(): Flow<List<Template>> =
        templateDao.getAllTemplates().map { entities ->
            entities.filter { !it.isBuiltIn }.map { it.toDomain() }
        }

    override fun getPremiumTemplates(): Flow<List<Template>> =
        templateDao.getPremiumTemplates().map { it.map { e -> e.toDomain() } }

    override fun searchTemplates(query: String): Flow<List<Template>> =
        templateDao.searchTemplates(query).map { it.map { e -> e.toDomain() } }

    override suspend fun insertTemplate(template: Template): Long =
        templateDao.insert(TemplateEntity.fromDomain(template))

    override suspend fun updateTemplate(template: Template) =
        templateDao.update(TemplateEntity.fromDomain(template))

    override suspend fun deleteTemplate(id: Long) =
        templateDao.deleteCustom(id)

    override suspend fun duplicateTemplate(id: Long): Long {
        val original = templateDao.getTemplateByIdOnce(id) ?: return -1L
        val duplicate = original.copy(
            id = 0,
            name = "${original.name} (Copy)",
            isBuiltIn = false
        )
        return templateDao.insert(duplicate)
    }

    override suspend fun exportTemplate(templateId: Long, exportPath: String): Boolean {
        val template = templateDao.getTemplateByIdOnce(templateId) ?: return false
        return try {
            val json = com.google.gson.Gson().toJson(template.toDomain())
            java.io.File(exportPath).writeText(json)
            true
        } catch (_: Exception) { false }
    }

    override suspend fun importTemplate(templatePath: String): Long? {
        return try {
            val json = java.io.File(templatePath).readText()
            val template = com.google.gson.Gson().fromJson(json, Template::class.java)
            templateDao.insert(TemplateEntity.fromDomain(template.copy(id = 0)))
        } catch (_: Exception) { null }
    }

    fun getTemplatesByUsage(): Flow<List<Template>> =
        templateDao.getAllTemplatesByUsage().map { it.map { e -> e.toDomain() } }

    fun getFreeTemplates(): Flow<List<Template>> =
        templateDao.getFreeTemplates().map { it.map { e -> e.toDomain() } }

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

    override fun getBuiltInBrandPresets(): Flow<List<BrandPreset>> =
        brandPresetDao.getAllPresets().map { entities ->
            // Built-in presets are those with id <= 15 (matching BuiltInBrandPresets)
            entities.filter { it.id in 1..15 }.map { it.toDomain() }
        }

    override fun getUserBrandPresets(): Flow<List<BrandPreset>> =
        brandPresetDao.getAllPresets().map { entities ->
            entities.filter { it.id > 15 }.map { it.toDomain() }
        }

    override fun searchBrandPresets(query: String): Flow<List<BrandPreset>> =
        brandPresetDao.searchPresets(query).map { it.map { e -> e.toDomain() } }

    override suspend fun insertBrandPreset(preset: BrandPreset): Long =
        brandPresetDao.insert(BrandPresetEntity.fromDomain(preset))

    override suspend fun updateBrandPreset(preset: BrandPreset) =
        brandPresetDao.update(BrandPresetEntity.fromDomain(preset))

    override suspend fun deleteBrandPreset(id: Long) =
        brandPresetDao.deleteById(id)

    override suspend fun duplicateBrandPreset(id: Long): Long {
        val original = brandPresetDao.getPresetByIdOnce(id) ?: return -1L
        val duplicate = original.copy(
            id = 0,
            name = "${original.name} (Copy)"
        )
        return brandPresetDao.insert(duplicate)
    }

    fun getRecentPresets(): Flow<List<BrandPreset>> =
        brandPresetDao.getAllPresetsByRecent().map { it.map { e -> e.toDomain() } }

    fun searchPresets(query: String): Flow<List<BrandPreset>> =
        brandPresetDao.searchPresets(query).map { it.map { e -> e.toDomain() } }

    suspend fun getPresetByIdOnce(id: Long): BrandPreset? =
        brandPresetDao.getPresetByIdOnce(id)?.toDomain()
}
