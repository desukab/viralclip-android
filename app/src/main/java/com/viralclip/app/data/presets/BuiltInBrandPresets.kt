package com.viralclip.app.data.presets

import com.viralclip.app.domain.model.Alignment
import com.viralclip.app.domain.model.BrandPreset
import com.viralclip.app.domain.model.CaptionAnimation
import com.viralclip.app.domain.model.CaptionPosition
import com.viralclip.app.domain.model.CaptionPreset
import com.viralclip.app.domain.model.CaptionShadow
import com.viralclip.app.domain.model.CaptionStyle
import com.viralclip.app.domain.model.FontWeight

object BuiltInBrandPresets {

    fun getAllPresets(): List<BrandPreset> = presets

    fun getPresetById(id: Long): BrandPreset? {
        return presets.find { it.id == id }
    }

    fun getPresetsByCategory(category: PresetCategory): List<BrandPreset> {
        return presets.filter { it.category == category.name }
    }

    enum class PresetCategory(val displayName: String) {
        SOCIAL_MEDIA("Social Media"),
        BUSINESS("Business"),
        CREATIVE("Creative"),
        LIFESTYLE("Lifestyle"),
        TECH("Tech & Gaming"),
        EDUCATION("Education")
    }

    private val presets = listOf(
        BrandPreset(
            id = 1,
            name = "ViralClip Default",
            primaryColor = 0xFF7C3AED,
            secondaryColor = 0xFFEC4899,
            accentColor = 0xFF3B82F6,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "Made with ViralClip",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.BOLD_HIGHLIGHT,
                fontSize = 32,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFFBBF24,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true
            )
        ),
        BrandPreset(
            id = 2,
            name = "Tech Influencer",
            category = PresetCategory.TECH.name,
            primaryColor = 0xFF06B6D4,
            secondaryColor = 0xFF3B82F6,
            accentColor = 0xFF10B981,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "@techinfluencer",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.NEON,
                fontSize = 30,
                fontWeight = FontWeight.SEMI_BOLD,
                fontColor = 0xFF06B6D4,
                highlightColor = 0xFF3B82F6,
                outlineColor = 0xFF000000,
                outlineWidth = 1f,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFF06B6D4,
                    offsetX = 0f,
                    offsetY = 0f,
                    blurRadius = 12f
                ),
                animation = CaptionAnimation.SCALE,
                wordHighlightEnabled = true
            )
        ),
        BrandPreset(
            id = 3,
            name = "Gaming Channel",
            category = PresetCategory.TECH.name,
            primaryColor = 0xFFEF4444,
            secondaryColor = 0xFFF59E0B,
            accentColor = 0xFF10B981,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "GAME ON",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.DRAMATIC,
                fontSize = 36,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFEF4444,
                outlineColor = 0xFF000000,
                outlineWidth = 3f,
                backgroundColor = 0xCC000000,
                backgroundCornerRadius = 8f,
                animation = CaptionAnimation.BOUNCE,
                wordHighlightEnabled = true,
                caseStyle = com.viralclip.app.domain.model.CaseStyle.UPPERCASE
            )
        ),
        BrandPreset(
            id = 4,
            name = "Business Pro",
            category = PresetCategory.BUSINESS.name,
            primaryColor = 0xFF1E40AF,
            secondaryColor = 0xFF3B82F6,
            accentColor = 0xFF06B6D4,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "Your Brand",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.PROFESSIONAL,
                fontSize = 28,
                fontWeight = FontWeight.MEDIUM,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0xCC1E40AF,
                backgroundCornerRadius = 4f,
                backgroundPadding = 10f,
                animation = CaptionAnimation.FADE,
                wordHighlightEnabled = false,
                alignment = Alignment.CENTER
            )
        ),
        BrandPreset(
            id = 5,
            name = "Corporate Clean",
            category = PresetCategory.BUSINESS.name,
            primaryColor = 0xFF1F2937,
            secondaryColor = 0xFF6B7280,
            accentColor = 0xFF10B981,
            fontFamily = "default",
            watermarkEnabled = false,
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.MINIMAL,
                fontSize = 26,
                fontWeight = FontWeight.NORMAL,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0x99000000,
                backgroundCornerRadius = 4f,
                animation = CaptionAnimation.FADE,
                wordHighlightEnabled = false,
                maxLines = 1
            )
        ),
        BrandPreset(
            id = 6,
            name = "Lifestyle Vlog",
            category = PresetCategory.LIFESTYLE.name,
            primaryColor = 0xFFEC4899,
            secondaryColor = 0xFFFBBF24,
            accentColor = 0xFF10B981,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "@lifestyle",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.BOLD_HIGHLIGHT,
                fontSize = 32,
                fontWeight = FontWeight.SEMI_BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFFBBF24,
                outlineColor = 0xFF000000,
                outlineWidth = 1f,
                animation = CaptionAnimation.WORD_HIGHLIGHT,
                wordHighlightEnabled = true
            )
        ),
        BrandPreset(
            id = 7,
            name = "Beauty & Fashion",
            category = PresetCategory.LIFESTYLE.name,
            primaryColor = 0xFFF472B6,
            secondaryColor = 0xFFFBBF24,
            accentColor = 0xFFFFFFFF,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "StyleDaily",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.GRADIENT,
                fontSize = 30,
                fontWeight = FontWeight.SEMI_BOLD,
                fontColor = 0xFFF472B6,
                highlightColor = 0xFFFBBF24,
                outlineColor = 0xFFFFFFFF,
                outlineWidth = 1f,
                animation = CaptionAnimation.SLIDE_UP,
                wordHighlightEnabled = false
            )
        ),
        BrandPreset(
            id = 8,
            name = "Food & Cooking",
            category = PresetCategory.LIFESTYLE.name,
            primaryColor = 0xFFF97316,
            secondaryColor = 0xFFFBBF24,
            accentColor = 0xFF10B981,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "YumTime",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.POP_IN,
                fontSize = 32,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFF97316,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                backgroundColor = 0xCC000000,
                backgroundCornerRadius = 12f,
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true
            )
        ),
        BrandPreset(
            id = 9,
            name = "Education Hub",
            category = PresetCategory.EDUCATION.name,
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF3B82F6,
            accentColor = 0xFFFBBF24,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "LearnDaily",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.PROFESSIONAL,
                fontSize = 30,
                fontWeight = FontWeight.SEMI_BOLD,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0xE610B981,
                backgroundCornerRadius = 8f,
                backgroundPadding = 12f,
                animation = CaptionAnimation.FADE,
                wordHighlightEnabled = true,
                position = CaptionPosition.BOTTOM,
                positionYPercent = 0.7f
            )
        ),
        BrandPreset(
            id = 10,
            name = "Creative Artist",
            category = PresetCategory.CREATIVE.name,
            primaryColor = 0xFF7C3AED,
            secondaryColor = 0xFFEC4899,
            accentColor = 0xFF06B6D4,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "@create",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.GRADIENT,
                fontSize = 36,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFF7C3AED,
                highlightColor = 0xFFEC4899,
                outlineColor = 0xFFFFFFFF,
                outlineWidth = 1f,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFF7C3AED,
                    offsetX = 0f,
                    offsetY = 0f,
                    blurRadius = 10f
                ),
                animation = CaptionAnimation.SCALE,
                wordHighlightEnabled = false
            )
        ),
        BrandPreset(
            id = 11,
            name = "Music Artist",
            category = PresetCategory.CREATIVE.name,
            primaryColor = 0xFFEC4899,
            secondaryColor = 0xFF7C3AED,
            accentColor = 0xFFFBBF24,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "MUSIC",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.KARAOKE,
                fontSize = 34,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFEC4899,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                animation = CaptionAnimation.KARAOKE,
                wordHighlightEnabled = true,
                caseStyle = com.viralclip.app.domain.model.CaseStyle.UPPERCASE
            )
        ),
        BrandPreset(
            id = 12,
            name = "Comedy Skits",
            category = PresetCategory.CREATIVE.name,
            primaryColor = 0xFFFBBF24,
            secondaryColor = 0xFFEF4444,
            accentColor = 0xFF10B981,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "LOL",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.BOUNCE,
                fontSize = 36,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFFBBF24,
                outlineColor = 0xFF000000,
                outlineWidth = 3f,
                backgroundColor = 0xDD000000,
                backgroundCornerRadius = 16f,
                animation = CaptionAnimation.BOUNCE,
                wordHighlightEnabled = true,
                caseStyle = com.viralclip.app.domain.model.CaseStyle.UPPERCASE
            )
        ),
        BrandPreset(
            id = 13,
            name = "Instagram Influencer",
            category = PresetCategory.SOCIAL_MEDIA.name,
            primaryColor = 0xFFE1306C,
            secondaryColor = 0xFFF77737,
            accentColor = 0xFFFCAF45,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "Follow @you",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.DEFAULT,
                fontSize = 30,
                fontWeight = FontWeight.SEMI_BOLD,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0xCC000000,
                backgroundCornerRadius = 6f,
                animation = CaptionAnimation.WORD_HIGHLIGHT,
                wordHighlightEnabled = true
            )
        ),
        BrandPreset(
            id = 14,
            name = "TikTok Creator",
            category = PresetCategory.SOCIAL_MEDIA.name,
            primaryColor = 0xFF00F2EA,
            secondaryColor = 0xFFFF0050,
            accentColor = 0xFFFFFFFF,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "@tiktok",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.BOLD_HIGHLIGHT,
                fontSize = 34,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFF00F2EA,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true
            )
        ),
        BrandPreset(
            id = 15,
            name = "YouTube Shorts",
            category = PresetCategory.SOCIAL_MEDIA.name,
            primaryColor = 0xFFFF0000,
            secondaryColor = 0xFFFFFFFF,
            accentColor = 0xFFFBBF24,
            fontFamily = "default",
            watermarkEnabled = true,
            watermarkText = "Subscribe!",
            defaultCaptionStyle = CaptionStyle(
                preset = CaptionPreset.DEFAULT,
                fontSize = 32,
                fontWeight = FontWeight.SEMI_BOLD,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0xB3000000,
                backgroundCornerRadius = 4f,
                animation = CaptionAnimation.WORD_HIGHLIGHT,
                wordHighlightEnabled = true,
                position = CaptionPosition.BOTTOM,
                positionYPercent = 0.75f
            )
        )
    )

    fun createCustomPreset(
        name: String,
        primaryColor: Long,
        secondaryColor: Long,
        accentColor: Long,
        watermarkText: String? = null
    ): BrandPreset {
        return BrandPreset(
            name = name,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor,
            watermarkEnabled = watermarkText != null,
            watermarkText = watermarkText,
            defaultCaptionStyle = CaptionStyle(
                fontColor = 0xFFFFFFFF,
                highlightColor = primaryColor
            )
        )
    }
}
