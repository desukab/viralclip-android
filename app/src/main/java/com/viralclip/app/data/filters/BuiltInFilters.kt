package com.viralclip.app.data.filters

import com.viralclip.app.domain.model.ClipFilters
import com.viralclip.app.domain.model.FilterPreset

object BuiltInFilters {

    fun getAllFilters(): List<FilterOption> = filters

    fun getFilterPreset(preset: FilterPreset): FilterOption {
        return filters.find { it.preset == preset } ?: filters.first()
    }

    fun getFiltersByCategory(category: FilterCategory): List<FilterOption> {
        return filters.filter { it.category == category }
    }

    enum class FilterCategory(val displayName: String) {
        ENHANCE("Enhance"),
        COLOR("Color Grading"),
        STYLE("Style"),
        MOOD("Mood")
    }

    data class FilterOption(
        val preset: FilterPreset,
        val displayName: String,
        val description: String,
        val category: FilterCategory,
        val settings: ClipFilters,
        val thumbnailTint: Long = 0xFFFFFFFF
    )

    private val filters = listOf(
        FilterOption(
            preset = FilterPreset.NONE,
            displayName = "Original",
            description = "No filter applied",
            category = FilterCategory.ENHANCE,
            settings = ClipFilters(
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
                hue = 0f,
                temperature = 0f
            ),
            thumbnailTint = 0xFFFFFFFF
        ),
        FilterOption(
            preset = FilterPreset.VIVID,
            displayName = "Vivid",
            description = "Enhanced colors and contrast for vibrant footage",
            category = FilterCategory.ENHANCE,
            settings = ClipFilters(
                brightness = 0.05f,
                contrast = 1.2f,
                saturation = 1.3f,
                hue = 0f,
                temperature = 0.05f
            ),
            thumbnailTint = 0xFFEC4899
        ),
        FilterOption(
            preset = FilterPreset.WARM,
            displayName = "Warm",
            description = "Golden, sunny tones for cozy atmosphere",
            category = FilterCategory.COLOR,
            settings = ClipFilters(
                brightness = 0.02f,
                contrast = 1.1f,
                saturation = 1.1f,
                hue = 0f,
                temperature = 0.3f,
                vignette = 0.1f
            ),
            thumbnailTint = 0xFFF97316
        ),
        FilterOption(
            preset = FilterPreset.COOL,
            displayName = "Cool",
            description = "Blue tones for a fresh, modern look",
            category = FilterCategory.COLOR,
            settings = ClipFilters(
                brightness = 0f,
                contrast = 1.1f,
                saturation = 0.95f,
                hue = 0f,
                temperature = -0.25f
            ),
            thumbnailTint = 0xFF3B82F6
        ),
        FilterOption(
            preset = FilterPreset.VINTAGE,
            displayName = "Vintage",
            description = "Nostalgic film look with faded blacks",
            category = FilterCategory.STYLE,
            settings = ClipFilters(
                brightness = 0f,
                contrast = 0.9f,
                saturation = 0.85f,
                hue = 0.02f,
                temperature = 0.1f,
                vignette = 0.3f,
                grain = 0.15f
            ),
            thumbnailTint = 0xFFD4A574
        ),
        FilterOption(
            preset = FilterPreset.NOIR,
            displayName = "Noir",
            description = "High contrast black and white with dramatic shadows",
            category = FilterCategory.STYLE,
            settings = ClipFilters(
                brightness = 0f,
                contrast = 1.5f,
                saturation = 0f,
                hue = 0f,
                temperature = 0f,
                vignette = 0.4f
            ),
            thumbnailTint = 0xFF1F2937
        ),
        FilterOption(
            preset = FilterPreset.FILM,
            displayName = "Film",
            description = "Cinematic film stock look",
            category = FilterCategory.STYLE,
            settings = ClipFilters(
                brightness = -0.02f,
                contrast = 1.15f,
                saturation = 0.9f,
                hue = 0.01f,
                temperature = 0.05f,
                vignette = 0.25f,
                grain = 0.1f
            ),
            thumbnailTint = 0xFFE5D4C0
        ),
        FilterOption(
            preset = FilterPreset.SUNSET,
            displayName = "Sunset",
            description = "Warm orange and pink sunset tones",
            category = FilterCategory.MOOD,
            settings = ClipFilters(
                brightness = 0.05f,
                contrast = 1.1f,
                saturation = 1.25f,
                hue = 0.05f,
                temperature = 0.35f,
                vignette = 0.15f
            ),
            thumbnailTint = 0xFFFF8C42
        ),
        FilterOption(
            preset = FilterPreset.DRAMATIC,
            displayName = "Dramatic",
            description = "High contrast with deep shadows",
            category = FilterCategory.MOOD,
            settings = ClipFilters(
                brightness = -0.05f,
                contrast = 1.4f,
                saturation = 1.1f,
                hue = 0f,
                temperature = -0.1f,
                vignette = 0.5f
            ),
            thumbnailTint = 0xFF374151
        ),
        FilterOption(
            preset = FilterPreset.RETRO,
            displayName = "Retro",
            description = "70s inspired color palette",
            category = FilterCategory.STYLE,
            settings = ClipFilters(
                brightness = 0.03f,
                contrast = 0.95f,
                saturation = 1.15f,
                hue = -0.03f,
                temperature = 0.15f,
                vignette = 0.2f,
                grain = 0.08f
            ),
            thumbnailTint = 0xFFD4A574
        ),
        FilterOption(
            preset = FilterPreset.BLEACH,
            displayName = "Bleach Bypass",
            description = "Desaturated with boosted contrast",
            category = FilterCategory.COLOR,
            settings = ClipFilters(
                brightness = 0f,
                contrast = 1.3f,
                saturation = 0.5f,
                hue = 0f,
                temperature = 0f,
                vignette = 0.3f
            ),
            thumbnailTint = 0xFF9CA3AF
        ),
        FilterOption(
            preset = FilterPreset.CROSS,
            displayName = "Cross Process",
            description = "Experimental color shift effect",
            category = FilterCategory.STYLE,
            settings = ClipFilters(
                brightness = 0.05f,
                contrast = 1.2f,
                saturation = 1.3f,
                hue = 0.08f,
                temperature = 0.1f,
                vignette = 0.2f
            ),
            thumbnailTint = 0xFFA78BFA
        ),
        FilterOption(
            preset = FilterPreset.TEAL_ORANGE,
            displayName = "Teal & Orange",
            description = "Hollywood movie look with complementary colors",
            category = FilterCategory.COLOR,
            settings = ClipFilters(
                brightness = 0f,
                contrast = 1.2f,
                saturation = 1.15f,
                hue = 0f,
                temperature = 0.05f,
                vignette = 0.25f
            ),
            thumbnailTint = 0xFF14B8A6
        ),
        FilterOption(
            preset = FilterPreset.CYBERPUNK,
            displayName = "Cyberpunk",
            description = "Futuristic neon aesthetic",
            category = FilterCategory.MOOD,
            settings = ClipFilters(
                brightness = 0.05f,
                contrast = 1.3f,
                saturation = 1.4f,
                hue = 0.1f,
                temperature = -0.15f,
                vignette = 0.4f
            ),
            thumbnailTint = 0xFF06B6D4
        ),
        FilterOption(
            preset = FilterPreset.PASTEL,
            displayName = "Pastel Dream",
            description = "Soft, dreamy pastel tones",
            category = FilterCategory.MOOD,
            settings = ClipFilters(
                brightness = 0.1f,
                contrast = 0.9f,
                saturation = 0.7f,
                hue = 0.02f,
                temperature = 0.08f,
                vignette = 0.1f
            ),
            thumbnailTint = 0xFFF9A8D4
        )
    )

    fun getPresetSettings(preset: FilterPreset): ClipFilters {
        return getFilterPreset(preset).settings
    }

    fun createCustomFilter(
        name: String,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        hue: Float = 0f,
        temperature: Float = 0f,
        vignette: Float = 0f
    ): ClipFilters {
        return ClipFilters(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            hue = hue,
            temperature = temperature,
            vignette = vignette,
            preset = FilterPreset.NONE
        )
    }

    fun blendFilters(filter1: ClipFilters, filter2: ClipFilters, ratio: Float): ClipFilters {
        val r = ratio.coerceIn(0f, 1f)
        return ClipFilters(
            brightness = lerp(filter1.brightness, filter2.brightness, r),
            contrast = lerp(filter1.contrast, filter2.contrast, r),
            saturation = lerp(filter1.saturation, filter2.saturation, r),
            hue = lerp(filter1.hue, filter2.hue, r),
            temperature = lerp(filter1.temperature, filter2.temperature, r),
            vignette = lerp(filter1.vignette, filter2.vignette, r),
            blur = lerp(filter1.blur, filter2.blur, r),
            sharpen = lerp(filter1.sharpen, filter2.sharpen, r),
            grain = lerp(filter1.grain, filter2.grain, r),
            preset = FilterPreset.NONE
        )
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
}
