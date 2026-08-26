package com.viralclip.app.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viralclip.app.domain.model.*

// ─── Type Converters ─────────────────────────────────────────────────

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type)

    @TypeConverter
    fun fromCaptionStyle(value: CaptionStyle): String = gson.toJson(value)

    @TypeConverter
    fun toCaptionStyle(value: String): CaptionStyle =
        gson.fromJson(value, CaptionStyle::class.java)

    @TypeConverter
    fun fromClipFilters(value: ClipFilters): String = gson.toJson(value)

    @TypeConverter
    fun toClipFilters(value: String): ClipFilters =
        gson.fromJson(value, ClipFilters::class.java)

    @TypeConverter
    fun fromTextOverlayList(value: List<TextOverlay>): String = gson.toJson(value)

    @TypeConverter
    fun toTextOverlayList(value: String): List<TextOverlay> =
        gson.fromJson(value, object : TypeToken<List<TextOverlay>>() {}.type)
}

// ─── Project Entity ──────────────────────────────────────────────────

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceVideoUri: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val duration: Long = 0L,
    val templateId: Long? = null,
    val brandPresetId: Long? = null
) {
    fun toDomain(clips: List<Clip> = emptyList()) = Project(
        id = id, name = name, sourceVideoUri = sourceVideoUri,
        createdAt = createdAt, updatedAt = updatedAt,
        thumbnailPath = thumbnailPath, duration = duration,
        clips = clips, templateId = templateId, brandPresetId = brandPresetId
    )

    companion object {
        fun fromDomain(project: Project) = ProjectEntity(
            id = project.id, name = project.name,
            sourceVideoUri = project.sourceVideoUri,
            createdAt = project.createdAt, updatedAt = project.updatedAt,
            thumbnailPath = project.thumbnailPath, duration = project.duration,
            templateId = project.templateId, brandPresetId = project.brandPresetId
        )
    }
}

// ─── Clip Entity ─────────────────────────────────────────────────────

@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
@TypeConverters(Converters::class)
data class ClipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String = "Clip",
    val sourceVideoUri: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val order: Int = 0,
    val viralityScore: Float = 0f,
    val captionStyle: CaptionStyle = CaptionStyle(),
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val filters: ClipFilters = ClipFilters(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val selected: Boolean = false
) {
    fun toDomain() = Clip(
        id = id, projectId = projectId, name = name,
        sourceVideoUri = sourceVideoUri,
        startTimeMs = startTimeMs, endTimeMs = endTimeMs,
        order = order, viralityScore = viralityScore,
        captionStyle = captionStyle, speed = speed,
        volume = volume, isMuted = isMuted,
        filters = filters, textOverlays = textOverlays,
        selected = selected
    )

    companion object {
        fun fromDomain(clip: Clip) = ClipEntity(
            id = clip.id, projectId = clip.projectId, name = clip.name,
            sourceVideoUri = clip.sourceVideoUri,
            startTimeMs = clip.startTimeMs, endTimeMs = clip.endTimeMs,
            order = clip.order, viralityScore = clip.viralityScore,
            captionStyle = clip.captionStyle, speed = clip.speed,
            volume = clip.volume, isMuted = clip.isMuted,
            filters = clip.filters, textOverlays = clip.textOverlays,
            selected = clip.selected
        )
    }
}

// ─── Caption Entity ──────────────────────────────────────────────────

@Entity(
    tableName = "captions",
    foreignKeys = [
        ForeignKey(
            entity = ClipEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("clipId")]
)
data class CaptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clipId: Long,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 1.0f,
    val speakerIndex: Int = 0
) {
    fun toDomain() = CaptionSegment(
        id = id, clipId = clipId, text = text,
        startTimeMs = startTimeMs, endTimeMs = endTimeMs,
        confidence = confidence, speakerIndex = speakerIndex
    )

    companion object {
        fun fromDomain(caption: CaptionSegment) = CaptionEntity(
            id = caption.id, clipId = caption.clipId,
            text = caption.text,
            startTimeMs = caption.startTimeMs,
            endTimeMs = caption.endTimeMs,
            confidence = caption.confidence,
            speakerIndex = caption.speakerIndex
        )
    }
}

// ─── Template Entity ─────────────────────────────────────────────────

@Entity(tableName = "templates")
@TypeConverters(Converters::class)
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: TemplateCategory,
    val captionStyle: CaptionStyle,
    val description: String,
    val thumbnailUrl: String? = null,
    val isBuiltIn: Boolean = true,
    val isPremium: Boolean = false
) {
    fun toDomain() = Template(
        id = id, name = name, category = category,
        captionStyle = captionStyle, description = description,
        thumbnailUrl = thumbnailUrl, isBuiltIn = isBuiltIn,
        isPremium = isPremium
    )

    companion object {
        fun fromDomain(template: Template) = TemplateEntity(
            id = template.id, name = template.name,
            category = template.category,
            captionStyle = template.captionStyle,
            description = template.description,
            thumbnailUrl = template.thumbnailUrl,
            isBuiltIn = template.isBuiltIn,
            isPremium = template.isPremium
        )
    }
}

// ─── Brand Preset Entity ─────────────────────────────────────────────

@Entity(tableName = "brand_presets")
@TypeConverters(Converters::class)
data class BrandPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryColor: Long = 0xFF7C3AED,
    val secondaryColor: Long = 0xFFEC4899,
    val accentColor: Long = 0xFF3B82F6,
    val fontFamily: String = "default",
    val logoPath: String? = null,
    val watermarkEnabled: Boolean = false,
    val watermarkText: String? = null,
    val introTemplateId: Long? = null,
    val outroTemplateId: Long? = null,
    val defaultCaptionStyle: CaptionStyle = CaptionStyle(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = BrandPreset(
        id = id, name = name,
        primaryColor = primaryColor, secondaryColor = secondaryColor,
        accentColor = accentColor, fontFamily = fontFamily,
        logoPath = logoPath, watermarkEnabled = watermarkEnabled,
        watermarkText = watermarkText,
        introTemplateId = introTemplateId, outroTemplateId = outroTemplateId,
        defaultCaptionStyle = defaultCaptionStyle,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(preset: BrandPreset) = BrandPresetEntity(
            id = preset.id, name = preset.name,
            primaryColor = preset.primaryColor,
            secondaryColor = preset.secondaryColor,
            accentColor = preset.accentColor,
            fontFamily = preset.fontFamily,
            logoPath = preset.logoPath,
            watermarkEnabled = preset.watermarkEnabled,
            watermarkText = preset.watermarkText,
            introTemplateId = preset.introTemplateId,
            outroTemplateId = preset.outroTemplateId,
            defaultCaptionStyle = preset.defaultCaptionStyle,
            createdAt = preset.createdAt
        )
    }
}
