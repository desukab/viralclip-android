package com.viralclip.app.data.database.dao

import androidx.room.*
import com.viralclip.app.data.database.entities.*
import com.viralclip.app.domain.model.TemplateCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdOnce(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentProjects(limit: Int): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun getRecentProjectsByOpen(limit: Int): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>): List<Long>

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET isArchived = :archived, updatedAt = :timestamp WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun touchLastOpened(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM projects WHERE isArchived = 0")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM projects WHERE isArchived = 0")
    fun getCountFlow(): Flow<Int>
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY `order` ASC")
    fun getClipsByProjectId(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun getClipsByProjectIdOnce(projectId: Long): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE id = :id")
    fun getClipById(id: Long): Flow<ClipEntity?>

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getClipByIdOnce(id: Long): ClipEntity?

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY viralityScore DESC")
    fun getClipsByScore(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId AND isExported = 1")
    fun getExportedClips(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE projectId = :projectId AND `selected` = 1")
    fun getSelectedClips(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT MAX(viralityScore) FROM clips WHERE projectId = :projectId")
    suspend fun getMaxViralityScore(projectId: Long): Float?

    @Query("SELECT * FROM clips ORDER BY createdAt DESC")
    fun getAllClips(): Flow<List<ClipEntity>>

    @Query("SELECT COUNT(*) FROM clips WHERE projectId = :projectId")
    fun getClipCount(projectId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: ClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clips: List<ClipEntity>)

    @Update
    suspend fun update(clip: ClipEntity)

    @Update
    suspend fun updateAll(clips: List<ClipEntity>)

    @Query("UPDATE clips SET `order` = :newOrder WHERE id = :clipId")
    suspend fun updateOrder(clipId: Long, newOrder: Int)

    @Query("UPDATE clips SET selected = :selected WHERE id = :clipId")
    suspend fun setSelected(clipId: Long, selected: Boolean)

    @Query("UPDATE clips SET exportedPath = :path, isExported = 1, exportProgress = 1.0 WHERE id = :clipId")
    suspend fun markExported(clipId: Long, path: String)

    @Query("UPDATE clips SET exportProgress = :progress WHERE id = :clipId")
    suspend fun updateExportProgress(clipId: Long, progress: Float)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clips WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: Long)

    @Query("DELETE FROM clips")
    suspend fun deleteAll()
}

@Dao
interface CaptionDao {
    @Query("SELECT * FROM captions WHERE clipId = :clipId ORDER BY startTimeMs ASC")
    fun getCaptionsByClipId(clipId: Long): Flow<List<CaptionEntity>>

    @Query("SELECT * FROM captions WHERE clipId = :clipId ORDER BY startTimeMs ASC")
    suspend fun getCaptionsByClipIdOnce(clipId: Long): List<CaptionEntity>

    @Query("SELECT * FROM captions WHERE id = :id")
    suspend fun getCaptionById(id: Long): CaptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(caption: CaptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(captions: List<CaptionEntity>)

    @Update
    suspend fun update(caption: CaptionEntity)

    @Query("DELETE FROM captions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM captions WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: Long)

    @Query("SELECT * FROM captions WHERE clipId = :clipId AND startTimeMs <= :timeMs AND endTimeMs >= :timeMs LIMIT 1")
    suspend fun getCaptionAtTime(clipId: Long, timeMs: Long): CaptionEntity?

    @Query("SELECT COUNT(*) FROM captions WHERE clipId = :clipId")
    suspend fun getCountForClip(clipId: Long): Int

    @Transaction
    suspend fun replaceForClip(clipId: Long, captions: List<CaptionEntity>) {
        deleteByClipId(clipId)
        if (captions.isNotEmpty()) insertAll(captions)
    }
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isBuiltIn = 1 OR isBuiltIn = 0 ORDER BY usageCount DESC, name ASC")
    fun getAllTemplatesByUsage(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE category = :category")
    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isPremium = 0 ORDER BY rating DESC")
    fun getFreeTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE isPremium = 1 ORDER BY rating DESC")
    fun getPremiumTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE name LIKE '%' || :query || '%'")
    fun searchTemplates(query: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    fun getTemplateById(id: Long): Flow<TemplateEntity?>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateByIdOnce(id: Long): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TemplateEntity>)

    @Update
    suspend fun update(template: TemplateEntity)

    @Query("UPDATE templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("UPDATE templates SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: Long, rating: Float)

    @Query("DELETE FROM templates WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustom(id: Long)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getCount(): Int
}

@Dao
interface BrandPresetDao {
    @Query("SELECT * FROM brand_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<BrandPresetEntity>>

    @Query("SELECT * FROM brand_presets ORDER BY updatedAt DESC")
    fun getAllPresetsByRecent(): Flow<List<BrandPresetEntity>>

    @Query("SELECT * FROM brand_presets WHERE id = :id")
    fun getPresetById(id: Long): Flow<BrandPresetEntity?>

    @Query("SELECT * FROM brand_presets WHERE id = :id")
    suspend fun getPresetByIdOnce(id: Long): BrandPresetEntity?

    @Query("SELECT * FROM brand_presets WHERE name LIKE '%' || :query || '%'")
    fun searchPresets(query: String): Flow<List<BrandPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: BrandPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<BrandPresetEntity>)

    @Update
    suspend fun update(preset: BrandPresetEntity)

    @Query("DELETE FROM brand_presets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM brand_presets")
    suspend fun getCount(): Int
}

@Dao
interface ProcessingJobDao {
    @Query("SELECT * FROM processing_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<ProcessingJobEntity>>

    @Query("SELECT * FROM processing_jobs WHERE status = :status ORDER BY createdAt ASC")
    fun getJobsByStatus(status: String): Flow<List<ProcessingJobEntity>>

    @Query("SELECT * FROM processing_jobs WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getJobsForProject(projectId: Long): Flow<List<ProcessingJobEntity>>

    @Query("SELECT * FROM processing_jobs WHERE id = :id")
    suspend fun getJobById(id: Long): ProcessingJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: ProcessingJobEntity): Long

    @Update
    suspend fun update(job: ProcessingJobEntity)

    @Query("UPDATE processing_jobs SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, progress: Float)

    @Query("UPDATE processing_jobs SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: Long, status: String = ProcessingJobEntity.STATUS_FAILED, error: String)

    @Query("UPDATE processing_jobs SET status = :status, completedAt = :timestamp WHERE id = :id")
    suspend fun markCompleted(id: Long, status: String = ProcessingJobEntity.STATUS_COMPLETED, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM processing_jobs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM processing_jobs WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND createdAt < :olderThan")
    suspend fun deleteOldJobs(olderThan: Long): Int

    @Query("SELECT COUNT(*) FROM processing_jobs WHERE status IN ('PENDING', 'RUNNING')")
    fun getActiveJobCount(): Flow<Int>
}

@Dao
interface ExportedAssetDao {
    @Query("SELECT * FROM exported_assets ORDER BY exportedAt DESC")
    fun getAllAssets(): Flow<List<ExportedAssetEntity>>

    @Query("SELECT * FROM exported_assets WHERE projectId = :projectId ORDER BY exportedAt DESC")
    fun getAssetsForProject(projectId: Long): Flow<List<ExportedAssetEntity>>

    @Query("SELECT * FROM exported_assets WHERE clipId = :clipId ORDER BY exportedAt DESC")
    fun getAssetsForClip(clipId: Long): Flow<List<ExportedAssetEntity>>

    @Query("SELECT * FROM exported_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): ExportedAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: ExportedAssetEntity): Long

    @Query("DELETE FROM exported_assets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exported_assets WHERE exportedAt < :olderThan")
    suspend fun deleteOldAssets(olderThan: Long): Int

    @Query("SELECT SUM(sizeBytes) FROM exported_assets")
    suspend fun getTotalSize(): Long?

    @Query("SELECT COUNT(*) FROM exported_assets")
    fun getCount(): Flow<Int>
}
