package com.viralclip.app.data

import com.google.gson.Gson
import com.viralclip.app.data.database.entities.Converters
import com.viralclip.app.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters
    private lateinit var gson: Gson

    @Before
    fun setup() {
        converters = Converters()
        gson = Gson()
    }

    @Test
    fun `fromStringList and toStringList are reversible`() {
        val list = listOf("apple", "banana", "cherry")
        val json = converters.fromStringList(list)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun `fromStringList handles empty list`() {
        val list = emptyList<String>()
        val json = converters.fromStringList(list)
        assertEquals("[]", json)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun `fromStringList handles unicode characters`() {
        val list = listOf("hello", "world", "привет", "世界")
        val json = converters.fromStringList(list)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun `fromStringList handles special characters`() {
        val list = listOf("test,comma", "test\"quote", "test\nnewline")
        val json = converters.fromStringList(list)
        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun `fromCaptionStyle and toCaptionStyle are reversible`() {
        val style = CaptionStyle(
            preset = CaptionPreset.BOLD_HIGHLIGHT,
            fontFamily = "Roboto",
            fontSize = 48,
            fontWeight = FontWeight.EXTRA_BOLD,
            fontColor = 0xFFFF0000,
            highlightColor = 0xFFFFFF00,
            outlineColor = 0xFF000000,
            outlineWidth = 3f,
            backgroundColor = 0x80000000,
            backgroundCornerRadius = 12f,
            backgroundPadding = 16f,
            position = CaptionPosition.TOP,
            positionYPercent = 0.2f,
            animation = CaptionAnimation.SCALE,
            maxLines = 3,
            alignment = Alignment.LEFT,
            shadow = CaptionShadow(true, 0xFF000000, 4f, 4f, 8f),
            wordHighlightEnabled = true,
            caseStyle = CaseStyle.UPPERCASE
        )
        val json = converters.fromCaptionStyle(style)
        val restored = converters.toCaptionStyle(json)
        assertEquals(style, restored)
    }

    @Test
    fun `fromCaptionStyle handles default values`() {
        val style = CaptionStyle()
        val json = converters.fromCaptionStyle(style)
        assertNotNull(json)
        val restored = converters.toCaptionStyle(json)
        assertEquals(style, restored)
    }

    @Test
    fun `fromClipFilters and toClipFilters are reversible`() {
        val filters = ClipFilters(
            brightness = 0.2f,
            contrast = 1.5f,
            saturation = 1.2f,
            hue = 15f,
            temperature = 0.1f,
            vignette = 0.3f,
            blur = 0f,
            sharpen = 0.5f,
            grain = 0.1f,
            preset = FilterPreset.VIVID
        )
        val json = converters.fromClipFilters(filters)
        val restored = converters.toClipFilters(json)
        assertEquals(filters, restored)
    }

    @Test
    fun `fromClipFilters handles default values`() {
        val filters = ClipFilters()
        val json = converters.fromClipFilters(filters)
        val restored = converters.toClipFilters(json)
        assertEquals(filters, restored)
    }

    @Test
    fun `fromTextOverlayList and toTextOverlayList are reversible`() {
        val overlays = listOf(
            TextOverlay(
                id = 1L,
                text = "First overlay",
                startTimeMs = 0L,
                endTimeMs = 2000L,
                positionX = 0.5f,
                positionY = 0.8f,
                fontSize = 24,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0x80000000,
                rotation = 0f,
                scale = 1f,
                animation = TextAnimation.FADE
            ),
            TextOverlay(
                id = 2L,
                text = "Second overlay",
                startTimeMs = 3000L,
                endTimeMs = 5000L,
                positionX = 0.3f,
                positionY = 0.2f,
                fontSize = 32,
                fontColor = 0xFFFF0000,
                backgroundColor = 0x00000000,
                rotation = -10f,
                scale = 1.2f,
                animation = TextAnimation.BOUNCE
            )
        )
        val json = converters.fromTextOverlayList(overlays)
        val restored = converters.toTextOverlayList(json)
        assertEquals(overlays, restored)
    }

    @Test
    fun `fromTextOverlayList handles empty list`() {
        val overlays = emptyList<TextOverlay>()
        val json = converters.fromTextOverlayList(overlays)
        val restored = converters.toTextOverlayList(json)
        assertEquals(overlays, restored)
    }

    @Test
    fun `fromExportSettings and toExportSettings are reversible`() {
        val settings = ExportSettings(
            platform = PlatformPreset.INSTAGRAM_REELS,
            quality = ExportQuality.ULTRA,
            fps = 60,
            format = VideoFormat.MOV,
            includeCaptions = true,
            watermark = "@myhandle"
        )
        val json = converters.fromExportSettings(settings)
        val restored = converters.toExportSettings(json)
        assertEquals(settings, restored)
    }

    @Test
    fun `fromExportSettings handles default values`() {
        val settings = ExportSettings()
        val json = converters.fromExportSettings(settings)
        val restored = converters.toExportSettings(json)
        assertEquals(settings, restored)
    }

    @Test
    fun `fromTemplateCategory and toTemplateCategory are reversible`() {
        TemplateCategory.values().forEach { category ->
            val json = converters.fromTemplateCategory(category)
            val restored = converters.toTemplateCategory(json)
            assertEquals(category, restored)
        }
    }

    @Test
    fun `toTemplateCategory handles invalid value`() {
        val restored = converters.toTemplateCategory("INVALID_CATEGORY")
        assertEquals(TemplateCategory.VIRAL, restored)
    }

    @Test
    fun `toTemplateCategory handles empty string`() {
        val restored = converters.toTemplateCategory("")
        assertEquals(TemplateCategory.VIRAL, restored)
    }

    @Test
    fun `toCaptionStyle handles null gracefully`() {
        val restored = converters.toCaptionStyle("null")
        assertEquals(CaptionStyle(), restored)
    }

    @Test
    fun `toClipFilters handles null gracefully`() {
        val restored = converters.toClipFilters("null")
        assertEquals(ClipFilters(), restored)
    }

    @Test
    fun `toExportSettings handles null gracefully`() {
        val restored = converters.toExportSettings("null")
        assertEquals(ExportSettings(), restored)
    }

    @Test
    fun `CaptionShadow equality works correctly`() {
        val shadow1 = CaptionShadow()
        val shadow2 = CaptionShadow()
        assertEquals(shadow1, shadow2)

        val shadow3 = shadow1.copy(color = 0xFFFF0000)
        assertNotEquals(shadow1, shadow3)
    }

    @Test
    fun `TextOverlay equality works correctly`() {
        val overlay1 = TextOverlay(text = "Test", startTimeMs = 0L, endTimeMs = 1000L)
        val overlay2 = overlay1.copy()
        assertEquals(overlay1, overlay2)

        val overlay3 = overlay1.copy(text = "Different")
        assertNotEquals(overlay1, overlay3)
    }

    @Test
    fun `FilterPreset has all expected values`() {
        val presets = FilterPreset.values()
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.contains(FilterPreset.NONE))
        assertTrue(presets.contains(FilterPreset.VIVID))
        assertTrue(presets.contains(FilterPreset.CYBERPUNK))
    }

    @Test
    fun `CaptionPreset has all expected values`() {
        val presets = CaptionPreset.values()
        assertTrue(presets.size >= 10)
        assertTrue(presets.contains(CaptionPreset.DEFAULT))
        assertTrue(presets.contains(CaptionPreset.BOLD_HIGHLIGHT))
        assertTrue(presets.contains(CaptionPreset.KARAOKE))
    }

    @Test
    fun `PlatformPreset has correct dimensions`() {
        assertEquals(1080, PlatformPreset.TIKTOK.width)
        assertEquals(1920, PlatformPreset.TIKTOK.height)
        assertEquals("9:16", PlatformPreset.TIKTOK.aspectRatio)

        assertEquals(1080, PlatformPreset.INSTAGRAM_FEED.width)
        assertEquals(1080, PlatformPreset.INSTAGRAM_FEED.height)
        assertEquals("1:1", PlatformPreset.INSTAGRAM_FEED.aspectRatio)
    }

    @Test
    fun `ExportQuality has correct bitrates`() {
        assertEquals(2_000_000, ExportQuality.LOW.bitrate)
        assertEquals(4_000_000, ExportQuality.MEDIUM.bitrate)
        assertEquals(8_000_000, ExportQuality.HIGH.bitrate)
        assertEquals(15_000_000, ExportQuality.ULTRA.bitrate)
    }
}
