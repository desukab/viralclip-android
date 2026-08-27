package com.viralclip.app.data

import com.viralclip.app.data.database.entities.*
import com.viralclip.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class EntityMappingTest {

    @Test
    fun `ProjectEntity toDomain converts all fields`() {
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = 42L,
            name = "Test Project",
            sourceVideoUri = "content://test",
            createdAt = now,
            updatedAt = now,
            thumbnailPath = "/path/to/thumb.jpg",
            duration = 60000L,
            templateId = 5L,
            brandPresetId = 3L
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.sourceVideoUri, domain.sourceVideoUri)
        assertEquals(entity.createdAt, domain.createdAt)
        assertEquals(entity.updatedAt, domain.updatedAt)
        assertEquals(entity.thumbnailPath, domain.thumbnailPath)
        assertEquals(entity.duration, domain.duration)
        assertEquals(entity.templateId, domain.templateId)
        assertEquals(entity.brandPresetId, domain.brandPresetId)
    }

    @Test
    fun `ProjectEntity toDomain passes through clips`() {
        val clips = listOf(
            Clip(id = 1L, projectId = 42L, name = "Clip 1"),
            Clip(id = 2L, projectId = 42L, name = "Clip 2")
        )
        val entity = ProjectEntity(id = 42L, name = "Test", sourceVideoUri = "test")

        val domain = entity.toDomain(clips)

        assertEquals(2, domain.clips.size)
        assertEquals("Clip 1", domain.clips[0].name)
    }

    @Test
    fun `ProjectEntity fromDomain converts all fields`() {
        val project = Project(
            id = 10L,
            name = "My Project",
            sourceVideoUri = "file:///video.mp4",
            createdAt = 1000L,
            updatedAt = 2000L,
            thumbnailPath = "/tmp/thumb.jpg",
            duration = 30000L,
            templateId = 7L,
            brandPresetId = 4L
        )

        val entity = ProjectEntity.fromDomain(project)

        assertEquals(project.id, entity.id)
        assertEquals(project.name, entity.name)
        assertEquals(project.sourceVideoUri, entity.sourceVideoUri)
        assertEquals(project.createdAt, entity.createdAt)
        assertEquals(project.updatedAt, entity.updatedAt)
        assertEquals(project.thumbnailPath, entity.thumbnailPath)
        assertEquals(project.duration, entity.duration)
        assertEquals(project.templateId, entity.templateId)
        assertEquals(project.brandPresetId, entity.brandPresetId)
    }

    @Test
    fun `ClipEntity toDomain converts all fields`() {
        val entity = ClipEntity(
            id = 1L,
            projectId = 10L,
            name = "Test Clip",
            sourceVideoUri = "content://clip",
            startTimeMs = 0L,
            endTimeMs = 5000L,
            order = 2,
            viralityScore = 0.85f,
            captionStyle = CaptionStyle(preset = CaptionPreset.BOLD_HIGHLIGHT),
            speed = 1.5f,
            volume = 0.8f,
            isMuted = true,
            filters = ClipFilters(brightness = 0.1f),
            textOverlays = listOf(
                TextOverlay(text = "Test", startTimeMs = 0L, endTimeMs = 2000L)
            ),
            selected = true
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.projectId, domain.projectId)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.sourceVideoUri, domain.sourceVideoUri)
        assertEquals(entity.startTimeMs, domain.startTimeMs)
        assertEquals(entity.endTimeMs, domain.endTimeMs)
        assertEquals(entity.order, domain.order)
        assertEquals(entity.viralityScore, domain.viralityScore, 0.001f)
        assertEquals(entity.captionStyle, domain.captionStyle)
        assertEquals(entity.speed, domain.speed, 0.001f)
        assertEquals(entity.volume, domain.volume, 0.001f)
        assertEquals(entity.isMuted, domain.isMuted)
        assertEquals(entity.filters, domain.filters)
        assertEquals(entity.textOverlays, domain.textOverlays)
        assertEquals(entity.selected, domain.selected)
    }

    @Test
    fun `ClipEntity fromDomain converts all fields`() {
        val clip = Clip(
            id = 5L,
            projectId = 20L,
            name = "My Clip",
            sourceVideoUri = "file:///test.mp4",
            startTimeMs = 1000L,
            endTimeMs = 10000L,
            order = 1,
            viralityScore = 0.75f,
            captionStyle = CaptionStyle(),
            speed = 2.0f,
            volume = 0.5f,
            isMuted = false,
            filters = ClipFilters(),
            textOverlays = emptyList(),
            selected = false
        )

        val entity = ClipEntity.fromDomain(clip)

        assertEquals(clip.id, entity.id)
        assertEquals(clip.projectId, entity.projectId)
        assertEquals(clip.name, entity.name)
        assertEquals(clip.sourceVideoUri, entity.sourceVideoUri)
        assertEquals(clip.startTimeMs, entity.startTimeMs)
        assertEquals(clip.endTimeMs, entity.endTimeMs)
        assertEquals(clip.order, entity.order)
        assertEquals(clip.viralityScore, entity.viralityScore, 0.001f)
    }

    @Test
    fun `CaptionEntity toDomain converts all fields`() {
        val entity = CaptionEntity(
            id = 1L,
            clipId = 10L,
            text = "Hello world",
            startTimeMs = 0L,
            endTimeMs = 2000L,
            confidence = 0.95f,
            speakerIndex = 1,
            language = "en"
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.clipId, domain.clipId)
        assertEquals(entity.text, domain.text)
        assertEquals(entity.startTimeMs, domain.startTimeMs)
        assertEquals(entity.endTimeMs, domain.endTimeMs)
        assertEquals(entity.confidence, domain.confidence, 0.001f)
        assertEquals(entity.speakerIndex, domain.speakerIndex)
    }

    @Test
    fun `CaptionEntity fromDomain converts all fields`() {
        val caption = CaptionSegment(
            id = 5L,
            clipId = 15L,
            text = "Test caption",
            startTimeMs = 1000L,
            endTimeMs = 3000L,
            confidence = 0.9f,
            speakerIndex = 2
        )

        val entity = CaptionEntity.fromDomain(caption)

        assertEquals(caption.id, entity.id)
        assertEquals(caption.clipId, entity.clipId)
        assertEquals(caption.text, entity.text)
        assertEquals(caption.startTimeMs, entity.startTimeMs)
        assertEquals(caption.endTimeMs, entity.endTimeMs)
        assertEquals(caption.confidence, entity.confidence, 0.001f)
        assertEquals(caption.speakerIndex, entity.speakerIndex)
    }

    @Test
    fun `TemplateEntity toDomain converts all fields`() {
        val entity = TemplateEntity(
            id = 1L,
            name = "Viral Template",
            category = TemplateCategory.VIRAL,
            captionStyle = CaptionStyle(),
            description = "Test description",
            thumbnailUrl = "https://example.com/thumb.jpg",
            isBuiltIn = true,
            isPremium = false,
            usageCount = 100,
            rating = 4.5f
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.category, domain.category)
        assertEquals(entity.captionStyle, domain.captionStyle)
        assertEquals(entity.description, domain.description)
        assertEquals(entity.thumbnailUrl, domain.thumbnailUrl)
        assertEquals(entity.isBuiltIn, domain.isBuiltIn)
        assertEquals(entity.isPremium, domain.isPremium)
    }

    @Test
    fun `TemplateEntity fromDomain converts all fields`() {
        val template = Template(
            id = 10L,
            name = "My Template",
            category = TemplateCategory.CINEMATIC,
            captionStyle = CaptionStyle(),
            description = "Custom template",
            thumbnailUrl = "thumb.jpg",
            isBuiltIn = false,
            isPremium = true
        )

        val entity = TemplateEntity.fromDomain(template)

        assertEquals(template.id, entity.id)
        assertEquals(template.name, entity.name)
        assertEquals(template.category, entity.category)
        assertEquals(template.captionStyle, entity.captionStyle)
        assertEquals(template.description, entity.description)
        assertEquals(template.thumbnailUrl, entity.thumbnailUrl)
        assertEquals(template.isBuiltIn, entity.isBuiltIn)
        assertEquals(template.isPremium, entity.isPremium)
    }

    @Test
    fun `BrandPresetEntity toDomain converts all fields`() {
        val entity = BrandPresetEntity(
            id = 1L,
            name = "My Brand",
            primaryColor = 0xFF7C3AED,
            secondaryColor = 0xFFEC4899,
            accentColor = 0xFF3B82F6,
            fontFamily = "Roboto",
            logoPath = "/path/to/logo.png",
            watermarkEnabled = true,
            watermarkText = "@myhandle",
            introTemplateId = 5L,
            outroTemplateId = 6L,
            defaultCaptionStyle = CaptionStyle()
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.primaryColor, domain.primaryColor)
        assertEquals(entity.secondaryColor, domain.secondaryColor)
        assertEquals(entity.accentColor, domain.accentColor)
        assertEquals(entity.fontFamily, domain.fontFamily)
        assertEquals(entity.logoPath, domain.logoPath)
        assertEquals(entity.watermarkEnabled, domain.watermarkEnabled)
        assertEquals(entity.watermarkText, domain.watermarkText)
        assertEquals(entity.introTemplateId, domain.introTemplateId)
        assertEquals(entity.outroTemplateId, domain.outroTemplateId)
    }

    @Test
    fun `BrandPresetEntity fromDomain converts all fields`() {
        val preset = BrandPreset(
            id = 10L,
            name = "Tech Brand",
            primaryColor = 0xFF3B82F6,
            secondaryColor = 0xFF06B6D4,
            accentColor = 0xFF10B981,
            fontFamily = "Inter",
            logoPath = "/logo.png",
            watermarkEnabled = false,
            watermarkText = null,
            introTemplateId = 7L,
            outroTemplateId = 8L,
            defaultCaptionStyle = CaptionStyle()
        )

        val entity = BrandPresetEntity.fromDomain(preset)

        assertEquals(preset.id, entity.id)
        assertEquals(preset.name, entity.name)
        assertEquals(preset.primaryColor, entity.primaryColor)
        assertEquals(preset.secondaryColor, entity.secondaryColor)
        assertEquals(preset.accentColor, entity.accentColor)
        assertEquals(preset.fontFamily, entity.fontFamily)
        assertEquals(preset.logoPath, entity.logoPath)
        assertEquals(preset.watermarkEnabled, entity.watermarkEnabled)
        assertEquals(preset.watermarkText, entity.watermarkText)
    }

    @Test
    fun `ProcessingJobEntity has correct status constants`() {
        assertEquals("PENDING", ProcessingJobEntity.STATUS_PENDING)
        assertEquals("RUNNING", ProcessingJobEntity.STATUS_RUNNING)
        assertEquals("COMPLETED", ProcessingJobEntity.STATUS_COMPLETED)
        assertEquals("FAILED", ProcessingJobEntity.STATUS_FAILED)
        assertEquals("CANCELLED", ProcessingJobEntity.STATUS_CANCELLED)
    }

    @Test
    fun `ProcessingJobEntity has correct type constants`() {
        assertEquals("PROCESS_VIDEO", ProcessingJobEntity.TYPE_PROCESS_VIDEO)
        assertEquals("EXPORT_CLIP", ProcessingJobEntity.TYPE_EXPORT_CLIP)
        assertEquals("GENERATE_CAPTIONS", ProcessingJobEntity.TYPE_GENERATE_CAPTIONS)
        assertEquals("TRANSCRIBE_AUDIO", ProcessingJobEntity.TYPE_TRANSCRIBE_AUDIO)
    }

    @Test
    fun `ExportedAssetEntity has correct defaults`() {
        val entity = ExportedAssetEntity(
            projectId = 1L,
            filePath = "/test.mp4",
            fileName = "test.mp4",
            mimeType = "video/mp4",
            sizeBytes = 1000L
        )

        assertEquals(0, entity.width)
        assertEquals(0, entity.height)
        assertEquals(0L, entity.durationMs)
        assertNotNull(entity.exportedAt)
    }

    @Test
    fun `ClipEntity fromDomain handles empty textOverlays`() {
        val clip = Clip(
            projectId = 1L,
            name = "Test",
            sourceVideoUri = "uri",
            startTimeMs = 0L,
            endTimeMs = 1000L,
            textOverlays = emptyList()
        )

        val entity = ClipEntity.fromDomain(clip)

        assertTrue(entity.textOverlays.isEmpty())
    }
}
