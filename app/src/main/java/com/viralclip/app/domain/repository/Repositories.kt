package com.viralclip.app.domain.repository

import android.net.Uri
import com.viralclip.app.domain.model.*
import com.viralclip.app.util.AppError
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    fun getProjectById(id: Long): Flow<Project?>
    fun getRecentProjects(limit: Int = 10): Flow<List<Project>>
    fun getProjectsByDateRange(startDate: Long, endDate: Long): Flow<List<Project>>
    fun searchProjects(query: String): Flow<List<Project>>
    fun getProjectCount(): Flow<Int>
    suspend fun insertProject(project: Project): Long
    suspend fun updateProject(project: Project)
    suspend fun deleteProject(id: Long)
    suspend fun deleteAllProjects()
    suspend fun duplicateProject(id: Long): Long
    suspend fun renameProject(id: Long, newName: String)
    suspend fun updateProjectThumbnail(projectId: Long, thumbnailPath: String)
    suspend fun archiveProject(id: Long)
    suspend fun unarchiveProject(id: Long)
}

interface ClipRepository {
    fun getAllClips(): Flow<List<Clip>>
    fun getClipsByProjectId(projectId: Long): Flow<List<Clip>>
    fun getClipById(id: Long): Flow<Clip?>
    fun getSelectedClips(projectId: Long): Flow<List<Clip>>
    fun getClipsByViralityScore(projectId: Long, minScore: Float): Flow<List<Clip>>
    suspend fun insertClip(clip: Clip): Long
    suspend fun insertClips(clips: List<Clip>)
    suspend fun updateClip(clip: Clip)
    suspend fun updateClips(clips: List<Clip>)
    suspend fun deleteClip(id: Long)
    suspend fun deleteClipsByProjectId(projectId: Long)
    suspend fun reorderClips(clipIds: List<Long>)
    suspend fun selectClip(clipId: Long, selected: Boolean)
    suspend fun selectAllClips(projectId: Long)
    suspend fun deselectAllClips(projectId: Long)
    suspend fun updateClipSpeed(clipId: Long, speed: Float)
    suspend fun updateClipVolume(clipId: Long, volume: Float)
    suspend fun updateClipCaptions(clipId: Long, captions: List<CaptionSegment>)
    suspend fun updateClipCaptionStyle(clipId: Long, style: CaptionStyle)
    suspend fun updateClipFilters(clipId: Long, filters: ClipFilters)
    suspend fun updateClipTrimPoints(clipId: Long, startTimeMs: Long, endTimeMs: Long)
}

interface CaptionRepository {
    fun getCaptionsByClipId(clipId: Long): Flow<List<CaptionSegment>>
    fun getCaptionsByTimeRange(clipId: Long, startMs: Long, endMs: Long): Flow<List<CaptionSegment>>
    fun getCaptionById(id: Long): Flow<CaptionSegment?>
    suspend fun insertCaptions(captions: List<CaptionSegment>)
    suspend fun insertCaption(caption: CaptionSegment): Long
    suspend fun updateCaption(caption: CaptionSegment)
    suspend fun updateCaptions(captions: List<CaptionSegment>)
    suspend fun deleteCaptionsByClipId(clipId: Long)
    suspend fun deleteCaption(id: Long)
    suspend fun updateCaptionStyle(clipId: Long, style: CaptionStyle)
    suspend fun updateCaptionText(captionId: Long, text: String)
    suspend fun updateCaptionTimeRange(captionId: Long, startTimeMs: Long, endTimeMs: Long)
    suspend fun mergeCaptions(captionIds: List<Long>): Long
    suspend fun splitCaption(captionId: Long, splitTimeMs: Long): Pair<Long, Long>
}

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<Template>>
    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<Template>>
    fun getTemplateById(id: Long): Flow<Template?>
    fun getBuiltInTemplates(): Flow<List<Template>>
    fun getUserTemplates(): Flow<List<Template>>
    fun getPremiumTemplates(): Flow<List<Template>>
    fun searchTemplates(query: String): Flow<List<Template>>
    suspend fun insertTemplate(template: Template): Long
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplate(id: Long)
    suspend fun duplicateTemplate(id: Long): Long
    suspend fun exportTemplate(templateId: Long, exportPath: String): Boolean
    suspend fun importTemplate(templatePath: String): Long?
}

interface BrandPresetRepository {
    fun getAllBrandPresets(): Flow<List<BrandPreset>>
    fun getBrandPresetById(id: Long): Flow<BrandPreset?>
    fun getBuiltInBrandPresets(): Flow<List<BrandPreset>>
    fun getUserBrandPresets(): Flow<List<BrandPreset>>
    fun searchBrandPresets(query: String): Flow<List<BrandPreset>>
    suspend fun insertBrandPreset(preset: BrandPreset): Long
    suspend fun updateBrandPreset(preset: BrandPreset)
    suspend fun deleteBrandPreset(id: Long)
    suspend fun duplicateBrandPreset(id: Long): Long
}

interface VideoRepository {
    suspend fun importVideo(uri: Uri): Result<VideoImportResult>
    suspend fun exportVideo(clip: Clip, settings: ExportSettings): Result<ExportResult>
    suspend fun generateThumbnail(videoUri: Uri, timeMs: Long): Result<String>
    suspend fun getVideoMetadata(uri: Uri): Result<VideoMetadata>
    suspend fun extractFrames(videoUri: Uri, intervalMs: Long): Result<List<String>>
    suspend fun analyzeVideo(videoUri: Uri): Result<VideoAnalysisResult>
    suspend fun transcribeAudio(videoUri: Uri): Result<List<CaptionSegment>>
    suspend fun detectFaces(videoUri: Uri): Result<List<FacePosition>>
    suspend fun calculateViralityScore(clip: Clip): Result<ViralityScore>
    suspend fun cancelProcessing()
    suspend fun getProcessingProgress(): Flow<Float>
}

interface ExportRepository {
    suspend fun exportClip(clip: Clip, settings: ExportSettings): Result<ExportResult>
    suspend fun exportClips(clips: List<Clip>, settings: ExportSettings): Result<List<ExportResult>>
    suspend fun getExportHistory(): Flow<List<ExportRecord>>
    suspend fun cancelExport(exportId: Long)
    suspend fun deleteExport(exportId: Long)
    suspend fun shareExport(exportId: Long, platform: String): Result<Unit>
    fun getExportFormats(): List<VideoFormat>
    fun getExportQualities(): List<ExportQuality>
}

interface SettingsRepository {
    fun getTheme(): Flow<AppTheme>
    fun getAutoSaveEnabled(): Flow<Boolean>
    fun getAutoSaveInterval(): Flow<Int>
    fun getDefaultExportQuality(): Flow<ExportQuality>
    fun getDefaultCaptionPreset(): Flow<CaptionPreset>
    fun getHapticFeedbackEnabled(): Flow<Boolean>
    fun getAnalyticsEnabled(): Flow<Boolean>
    fun getNotificationsEnabled(): Flow<Boolean>
    fun getStorageLimit(): Flow<Long>
    fun getCacheSize(): Flow<Long>
    suspend fun setTheme(theme: AppTheme)
    suspend fun setAutoSaveEnabled(enabled: Boolean)
    suspend fun setAutoSaveInterval(intervalSeconds: Int)
    suspend fun setDefaultExportQuality(quality: ExportQuality)
    suspend fun setDefaultCaptionPreset(preset: CaptionPreset)
    suspend fun setHapticFeedbackEnabled(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setStorageLimit(limitBytes: Long)
    suspend fun clearCache()
    suspend fun resetToDefaults()
}

interface AnalyticsRepository {
    suspend fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap())
    suspend fun trackScreen(screenName: String, properties: Map<String, Any> = emptyMap())
    suspend fun trackError(errorName: String, throwable: Throwable? = null, properties: Map<String, Any> = emptyMap())
    suspend fun setUserProperty(key: String, value: Any)
    suspend fun getUserProperties(): Map<String, Any>
    suspend fun getEventCounts(): Map<String, Long>
    suspend fun getRecentEvents(limit: Int = 100): List<AnalyticsRecord>
    suspend fun flush()
}

interface CacheRepository {
    fun getCacheSize(): Flow<Long>
    fun getMaxCacheSize(): Long
    suspend fun setMaxCacheSize(maxSizeBytes: Long)
    suspend fun clearCache()
    suspend fun clearThumbnailCache()
    suspend fun clearTempFiles()
    suspend fun pruneOldCache()
    fun getCachedThumbnail(videoUri: String, timeMs: Long): String?
    suspend fun cacheThumbnail(videoUri: String, timeMs: Long, path: String)
    suspend fun invalidateThumbnail(videoUri: String)
    suspend fun getCacheStats(): CacheStats
}

interface ErrorRepository {
    suspend fun logError(error: AppError)
    suspend fun getRecentErrors(limit: Int = 50): List<AppError>
    suspend fun clearErrors()
    suspend fun reportError(errorId: Long): Result<Unit>
    fun getErrorCategories(): List<ErrorCategory>
    fun getErrorSeverityLevels(): List<ErrorSeverity>
}

data class VideoImportResult(
    val uri: Uri,
    val name: String,
    val size: Long,
    val duration: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val thumbnailPath: String?
)

data class ExportResult(
    val exportId: Long,
    val filePath: String,
    val fileSize: Long,
    val duration: Long,
    val quality: ExportQuality,
    val format: VideoFormat,
    val createdAt: Long
)

data class VideoMetadata(
    val width: Int,
    val height: Int,
    val duration: Long,
    val frameRate: Float,
    val bitrate: Int,
    val codec: String,
    val audioCodec: String?,
    val hasAudio: Boolean,
    val fileSize: Long
)

data class VideoAnalysisResult(
    val frames: List<FrameAnalysis>,
    val viralityScore: ViralityScore,
    val suggestedClipPoints: List<ClipPoint>,
    val facesDetected: List<FacePosition>,
    val transcript: List<CaptionSegment>
)

data class ClipPoint(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val reason: String,
    val score: Float
)

data class ExportRecord(
    val id: Long,
    val clipId: Long,
    val projectId: Long,
    val filePath: String,
    val fileSize: Long,
    val duration: Long,
    val quality: ExportQuality,
    val format: VideoFormat,
    val platform: PlatformPreset,
    val exportedAt: Long,
    val shareUrl: String?
)

data class CacheStats(
    val totalSize: Long,
    val thumbnailCacheSize: Long,
    val tempCacheSize: Long,
    val fileCount: Int,
    val oldestFile: Long?,
    val newestFile: Long?
)

data class AnalyticsRecord(
    val id: Long,
    val name: String,
    val category: String,
    val timestamp: Long,
    val properties: Map<String, Any>,
    val userId: String?,
    val sessionId: String?
)

enum class AppTheme(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class ErrorCategory(val displayName: String) {
    VIDEO_IMPORT("Video Import"),
    VIDEO_EXPORT("Video Export"),
    PROCESSING("Processing"),
    CAPTION("Caption"),
    TEMPLATE("Template"),
    STORAGE("Storage"),
    NETWORK("Network"),
    UNKNOWN("Unknown")
}

enum class ErrorSeverity(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical")
}

data class ErrorLogEntry(
    val id: Long = 0,
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val message: String,
    val stackTrace: String?,
    val deviceInfo: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val resolvedAt: Long? = null
)
