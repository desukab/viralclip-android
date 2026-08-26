package com.viralclip.app.data.database.dao

import androidx.room.*
import com.viralclip.app.data.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentProjects(limit: Int): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getCount(): Int
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY `order` ASC")
    fun getClipsByProjectId(projectId: Long): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE id = :id")
    fun getClipById(id: Long): Flow<ClipEntity?>

    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY viralityScore DESC")
    fun getClipsByScore(projectId: Long): Flow<List<ClipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clip: ClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clips: List<ClipEntity>)

    @Update
    suspend fun update(clip: ClipEntity)

    @Update
    suspend fun updateAll(clips: List<ClipEntity>)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clips WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: Long)

    @Query("UPDATE clips SET `order` = :newOrder WHERE id = :clipId")
    suspend fun updateOrder(clipId: Long, newOrder: Int)
}

@Dao
interface CaptionDao {
    @Query("SELECT * FROM captions WHERE clipId = :clipId ORDER BY startTimeMs ASC")
    fun getCaptionsByClipId(clipId: Long): Flow<List<CaptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(captions: List<CaptionEntity>)

    @Update
    suspend fun update(caption: CaptionEntity)

    @Query("DELETE FROM captions WHERE clipId = :clipId")
    suspend fun deleteByClipId(clipId: Long)

    @Query("SELECT * FROM captions WHERE clipId = :clipId AND startTimeMs <= :timeMs AND endTimeMs >= :timeMs")
    suspend fun getCaptionAtTime(clipId: Long, timeMs: Long): CaptionEntity?
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE category = :category")
    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    fun getTemplateById(id: Long): Flow<TemplateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Query("DELETE FROM templates WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustom(id: Long)
}

@Dao
interface BrandPresetDao {
    @Query("SELECT * FROM brand_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<BrandPresetEntity>>

    @Query("SELECT * FROM brand_presets WHERE id = :id")
    fun getPresetById(id: Long): Flow<BrandPresetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: BrandPresetEntity): Long

    @Update
    suspend fun update(preset: BrandPresetEntity)

    @Query("DELETE FROM brand_presets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
