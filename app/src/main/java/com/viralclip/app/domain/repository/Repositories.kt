package com.viralclip.app.domain.repository

import com.viralclip.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    fun getRecentProjects(limit: Int = 10): Flow<List<Project>>
    suspend fun insertProject(project: Project): Long
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: Long)
    suspend fun duplicateProject(id: Long): Long
    suspend fun renameProject(id: Long, newName: String)
}

interface ClipRepository {
    fun getClipsByProjectId(projectId: Long): Flow<List<Clip>>
    fun getClipById(id: Long): Flow<Clip?>
    suspend fun insertClip(clip: Clip): Long
    suspend fun insertClips(clips: List<Clip>)
    suspend fun updateClip(clip: Clip)
    suspend fun updateClips(clips: List<Clip>)
    suspend fun deleteClip(id: Long)
    suspend fun reorderClips(clipIds: List<Long>)
}

interface CaptionRepository {
    fun getCaptionsByClipId(clipId: Long): Flow<List<CaptionSegment>>
    suspend fun insertCaptions(captions: List<CaptionSegment>)
    suspend fun updateCaption(caption: CaptionSegment)
    suspend fun deleteCaptionsByClipId(clipId: Long)
    suspend fun updateCaptionStyle(clipId: Long, style: CaptionStyle)
}

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<Template>>
    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<Template>>
    fun getTemplateById(id: Long): Flow<Template?>
    suspend fun insertTemplate(template: Template): Long
    suspend fun deleteTemplate(id: Long)
}

interface BrandPresetRepository {
    fun getAllBrandPresets(): Flow<List<BrandPreset>>
    fun getBrandPresetById(id: Long): Flow<BrandPreset?>
    suspend fun insertBrandPreset(preset: BrandPreset): Long
    suspend fun updateBrandPreset(preset: BrandPreset)
    suspend fun deleteBrandPreset(id: Long)
}
