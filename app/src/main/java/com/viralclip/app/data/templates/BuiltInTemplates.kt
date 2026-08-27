package com.viralclip.app.data.templates

import com.viralclip.app.domain.model.*

object BuiltInTemplates {

    fun getAllTemplates(): List<Template> = templates

    fun getTemplatesByCategory(category: TemplateCategory): List<Template> {
        return templates.filter { it.category == category }
    }

    fun getTemplateById(id: Long): Template? {
        return templates.find { it.id == id }
    }

    fun getViralTemplates(): List<Template> {
        return templates.filter { it.category == TemplateCategory.VIRAL }
    }

    fun getProfessionalTemplates(): List<Template> {
        return templates.filter { it.category == TemplateCategory.PROFESSIONAL }
    }

    fun getCreativeTemplates(): List<Template> {
        return templates.filter { it.category == TemplateCategory.CREATIVE }
    }

    private val templates = listOf(
        Template(
            id = 1,
            name = "Viral Hook",
            category = TemplateCategory.VIRAL,
            description = "Bold, attention-grabbing style perfect for viral content",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.BOLD_HIGHLIGHT,
                fontSize = 36,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFFBBF24,
                outlineWidth = 3f,
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 2,
            name = "TikTok Classic",
            category = TemplateCategory.VIRAL,
            description = "The classic TikTok style that started it all",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.DEFAULT,
                fontSize = 32,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0x99000000,
                backgroundCornerRadius = 6f,
                animation = CaptionAnimation.WORD_HIGHLIGHT,
                wordHighlightEnabled = true
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 3,
            name = "Engagement Booster",
            category = TemplateCategory.VIRAL,
            description = "Designed to maximize viewer engagement and shares",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.BOUNCE,
                fontSize = 34,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFEC4899,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                animation = CaptionAnimation.BOUNCE,
                wordHighlightEnabled = true,
                caseStyle = CaseStyle.UPPERCASE
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 4,
            name = "Minimal Pro",
            category = TemplateCategory.PROFESSIONAL,
            description = "Clean, minimal style for professional content",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.MINIMAL,
                fontSize = 28,
                fontWeight = FontWeight.MEDIUM,
                fontColor = 0xFFFFFFFF,
                animation = CaptionAnimation.FADE,
                wordHighlightEnabled = false,
                maxLines = 1
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 5,
            name = "Corporate Clean",
            category = TemplateCategory.PROFESSIONAL,
            description = "Professional captions for business content",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.PROFESSIONAL,
                fontSize = 30,
                fontWeight = FontWeight.MEDIUM,
                fontColor = 0xFFFFFFFF,
                backgroundColor = 0xCC1E40AF,
                backgroundCornerRadius = 4f,
                backgroundPadding = 8f,
                animation = CaptionAnimation.NONE,
                wordHighlightEnabled = false,
                alignment = Alignment.CENTER
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 6,
            name = "Executive",
            category = TemplateCategory.PROFESSIONAL,
            description = "Elegant style for executive presence",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.SHADOW,
                fontSize = 32,
                fontWeight = FontWeight.NORMAL,
                fontColor = 0xFFFFFFFF,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFF000000,
                    offsetX = 3f,
                    offsetY = 3f,
                    blurRadius = 6f
                ),
                animation = CaptionAnimation.SLIDE_UP,
                wordHighlightEnabled = false
            ),
            isBuiltIn = true,
            isPremium = true
        ),
        Template(
            id = 7,
            name = "Neon Nights",
            category = TemplateCategory.NEON,
            description = "Glowing neon effect for retro-futuristic vibes",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.NEON,
                fontSize = 36,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFF06B6D4,
                highlightColor = 0xFFEC4899,
                outlineColor = 0xFF06B6D4,
                outlineWidth = 4f,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFF06B6D4,
                    offsetX = 0f,
                    offsetY = 0f,
                    blurRadius = 15f
                ),
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 8,
            name = "Cyberpunk",
            category = TemplateCategory.NEON,
            description = "Futuristic cyberpunk aesthetic",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.NEON,
                fontSize = 38,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFFFF00FF,
                highlightColor = 0xFF00FFFF,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFFFF00FF,
                    offsetX = 0f,
                    offsetY = 0f,
                    blurRadius = 20f
                ),
                animation = CaptionAnimation.GLITCH,
                wordHighlightEnabled = false
            ),
            isBuiltIn = true,
            isPremium = true
        ),
        Template(
            id = 9,
            name = "Cinematic Wide",
            category = TemplateCategory.CINEMATIC,
            description = "Movie-style captions for dramatic storytelling",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.PROFESSIONAL,
                fontSize = 28,
                fontWeight = FontWeight.NORMAL,
                fontColor = 0xFFE5E5E5,
                backgroundColor = 0x66000000,
                backgroundCornerRadius = 2f,
                backgroundPadding = 12f,
                position = CaptionPosition.BOTTOM,
                positionYPercent = 0.85f,
                animation = CaptionAnimation.FADE,
                wordHighlightEnabled = false,
                alignment = Alignment.LEFT
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 10,
            name = "Film Noir",
            category = TemplateCategory.CINEMATIC,
            description = "Classic noir style for dramatic effect",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.OUTLINE,
                fontSize = 34,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                outlineColor = 0xFF000000,
                outlineWidth = 4f,
                animation = CaptionAnimation.SLIDE_UP,
                wordHighlightEnabled = false
            ),
            isBuiltIn = true,
            isPremium = true
        ),
        Template(
            id = 11,
            name = "Retro Wave",
            category = TemplateCategory.RETRO,
            description = "80s retro aesthetic with vibrant gradients",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.RETRO,
                fontSize = 36,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFFFF6B6B,
                highlightColor = 0xFF4ECDC4,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFFFF6B6B,
                    offsetX = 4f,
                    offsetY = 4f,
                    blurRadius = 8f
                ),
                animation = CaptionAnimation.BOUNCE,
                wordHighlightEnabled = true,
                caseStyle = CaseStyle.UPPERCASE
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 12,
            name = "VHS Nostalgia",
            category = TemplateCategory.RETRO,
            description = "VHS tape aesthetic for nostalgic content",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.OUTLINE,
                fontSize = 32,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                outlineColor = 0xFF000000,
                outlineWidth = 3f,
                backgroundColor = 0xAA000000,
                backgroundCornerRadius = 0f,
                animation = CaptionAnimation.TYPEWRITER,
                wordHighlightEnabled = false
            ),
            isBuiltIn = true,
            isPremium = true
        ),
        Template(
            id = 13,
            name = "Playful Pop",
            category = TemplateCategory.PLAYFUL,
            description = "Fun, bouncy style for lighthearted content",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.BOUNCE,
                fontSize = 34,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFBBF24,
                highlightColor = 0xFFEC4899,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                backgroundColor = 0xCC000000,
                backgroundCornerRadius = 16f,
                animation = CaptionAnimation.BOUNCE,
                wordHighlightEnabled = true,
                caseStyle = CaseStyle.TITLE_CASE
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 14,
            name = "Kawaii",
            category = TemplateCategory.PLAYFUL,
            description = "Cute Japanese-inspired style",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.POP_IN,
                fontSize = 30,
                fontWeight = FontWeight.MEDIUM,
                fontColor = 0xFFFF69B4,
                highlightColor = 0xFF87CEEB,
                outlineColor = 0xFFFFFFFF,
                outlineWidth = 1f,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFFFF69B4,
                    offsetX = 2f,
                    offsetY = 2f,
                    blurRadius = 4f
                ),
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 15,
            name = "Elegant Serif",
            category = TemplateCategory.ELEGANT,
            description = "Sophisticated serif typography",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.SHADOW,
                fontSize = 32,
                fontWeight = FontWeight.NORMAL,
                fontColor = 0xFFF5F5F5,
                shadow = CaptionShadow(
                    enabled = true,
                    color = 0xFF000000,
                    offsetX = 2f,
                    offsetY = 2f,
                    blurRadius = 3f
                ),
                animation = CaptionAnimation.FADE,
                wordHighlightEnabled = false,
                alignment = Alignment.CENTER
            ),
            isBuiltIn = true,
            isPremium = true
        ),
        Template(
            id = 16,
            name = "Gradient Glow",
            category = TemplateCategory.CREATIVE,
            description = "Beautiful gradient text effect",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.GRADIENT,
                fontSize = 38,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFF7C3AED,
                highlightColor = 0xFFEC4899,
                outlineColor = 0xFF000000,
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
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 17,
            name = "Dynamic Bold",
            category = TemplateCategory.BOLD,
            description = "High-impact bold style for maximum attention",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.BOLD_HIGHLIGHT,
                fontSize = 40,
                fontWeight = FontWeight.EXTRA_BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFEF4444,
                outlineColor = 0xFF000000,
                outlineWidth = 4f,
                backgroundColor = 0xDD000000,
                backgroundCornerRadius = 8f,
                animation = CaptionAnimation.POP_IN,
                wordHighlightEnabled = true,
                caseStyle = CaseStyle.UPPERCASE
            ),
            isBuiltIn = true,
            isPremium = false
        ),
        Template(
            id = 18,
            name = "Karaoke Night",
            category = TemplateCategory.CREATIVE,
            description = "Sing-along style with word-by-word highlighting",
            captionStyle = CaptionStyle(
                preset = CaptionPreset.KARAOKE,
                fontSize = 32,
                fontWeight = FontWeight.BOLD,
                fontColor = 0xFFFFFFFF,
                highlightColor = 0xFFFBBF24,
                outlineColor = 0xFF000000,
                outlineWidth = 2f,
                backgroundColor = 0x99000000,
                backgroundCornerRadius = 8f,
                animation = CaptionAnimation.KARAOKE,
                wordHighlightEnabled = true
            ),
            isBuiltIn = true,
            isPremium = false
        )
    )
}
