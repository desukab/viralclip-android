package com.viralclip.app.domain.model

import android.net.Uri

// ─── Core Domain Models ──────────────────────────────────────────────

data class Project(
    val id: Long = 0,
    val name: String,
    val sourceVideoUri: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val duration: Long = 0L,
    val clips: List<Clip> = emptyList(),
    val templateId: Long? = null,
    val brandPresetId: Long? = null
)

data class Clip(
    val id: Long = 0,
    val projectId: Long,
    val name: String = "Clip",
    val sourceVideoUri: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val order: Int = 0,
    val viralityScore: Float = 0f,
    val captions: List<CaptionSegment> = emptyList(),
    val captionStyle: CaptionStyle = CaptionStyle(),
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val filters: ClipFilters = ClipFilters(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val selected: Boolean = false
) {
    val durationMs: Long get() = endTimeMs - startTimeMs
    val durationSeconds: Float get() = durationMs / 1000f
}

data class CaptionSegment(
    val id: Long = 0,
    val clipId: Long = 0,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 1.0f,
    val speakerIndex: Int = 0
) {
    val durationMs: Long get() = endTimeMs - startTimeMs
    val words: List<CaptionWord> get() = text.split(" ").mapIndexed { index, word ->
        CaptionWord(
            text = word,
            startTimeMs = startTimeMs + (index * durationMs / text.split(" ").size),
            endTimeMs = startTimeMs + ((index + 1) * durationMs / text.split(" ").size)
        )
    }
}

data class CaptionWord(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

// ─── Caption Styles ──────────────────────────────────────────────────

data class CaptionStyle(
    val preset: CaptionPreset = CaptionPreset.DEFAULT,
    val fontFamily: String = "default",
    val fontSize: Int = 32,
    val fontWeight: FontWeight = FontWeight.BOLD,
    val fontColor: Long = 0xFFFFFFFF,
    val highlightColor: Long = 0xFFFBBF24,
    val outlineColor: Long = 0xFF000000,
    val outlineWidth: Float = 2f,
    val backgroundColor: Long = 0x80000000,
    val backgroundCornerRadius: Float = 8f,
    val backgroundPadding: Float = 8f,
    val position: CaptionPosition = CaptionPosition.BOTTOM,
    val positionYPercent: Float = 0.75f,
    val animation: CaptionAnimation = CaptionAnimation.WORD_HIGHLIGHT,
    val maxLines: Int = 2,
    val alignment: Alignment = Alignment.CENTER,
    val shadow: CaptionShadow = CaptionShadow(),
    val wordHighlightEnabled: Boolean = true,
    val caseStyle: CaseStyle = CaseStyle.NORMAL
)

enum class CaptionPreset(val displayName: String) {
    DEFAULT("Default"),
    BOLD_HIGHLIGHT("Bold Highlight"),
    KARAOKE("Karaoke"),
    TYPEWRITER("Typewriter"),
    POP_IN("Pop In"),
    BOUNCE("Bounce"),
    NEON("Neon Glow"),
    MINIMAL("Minimal"),
    PROFESSIONAL("Professional"),
    DRAMATIC("Dramatic"),
    HANDWRITTEN("Handwritten"),
    RETRO("Retro Wave"),
    GRADIENT("Gradient Text"),
    OUTLINE("Outline Only"),
    SHADOW("Shadow Text")
}

enum class FontWeight { LIGHT, NORMAL, MEDIUM, SEMI_BOLD, BOLD, EXTRA_BOLD }

enum class CaptionPosition(val displayName: String) {
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom"),
    CUSTOM("Custom")
}

enum class CaptionAnimation(val displayName: String) {
    NONE("None"),
    WORD_HIGHLIGHT("Word Highlight"),
    KARAOKE("Karaoke"),
    TYPEWRITER("Typewriter"),
    POP_IN("Pop In"),
    BOUNCE("Bounce"),
    FADE("Fade In/Out"),
    SCALE("Scale"),
    SLIDE_UP("Slide Up"),
    GLITCH("Glitch")
}

enum class Alignment { LEFT, CENTER, RIGHT }

enum class CaseStyle { NORMAL, UPPERCASE, LOWERCASE, TITLE_CASE, FIRST_WORD_CAPS }

data class CaptionShadow(
    val enabled: Boolean = true,
    val color: Long = 0xFF000000,
    val offsetX: Float = 2f,
    val offsetY: Float = 2f,
    val blurRadius: Float = 4f
)

// ─── Video Analysis ──────────────────────────────────────────────────

data class FrameAnalysis(
    val timestampMs: Long,
    val brightness: Float,
    val motionScore: Float,
    val faceCount: Int,
    val facePositions: List<FacePosition>,
    val sceneType: SceneType,
    val speechDetected: Boolean,
    val engagementScore: Float
)

data class FacePosition(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val confidence: Float
)

enum class SceneType { SPEECH, ACTION, TRANSITION, STATIC, MULTI_SPEAKER, UNKNOWN }

data class ViralityScore(
    val overall: Float,        // 0.0 - 1.0
    val engagementPotential: Float,
    val emotionalImpact: Float,
    val shareability: Float,
    val watchTime: Float,
    val hookStrength: Float,
    val reasons: List<String>,
    val suggestedStartTime: Long,
    val suggestedEndTime: Long
) {
    val label: String get() = when {
        overall >= 0.7f -> "High"
        overall >= 0.4f -> "Medium"
        else -> "Low"
    }
    val percentage: Int get() = (overall * 100).toInt()
}

// ─── Text Overlays ───────────────────────────────────────────────────

data class TextOverlay(
    val id: Long = 0,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val fontSize: Int = 24,
    val fontColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0x00000000,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val animation: TextAnimation = TextAnimation.NONE
)

enum class TextAnimation { NONE, FADE, SLIDE, BOUNCE, TYPEWRITER }

// ─── Filters ─────────────────────────────────────────────────────────

data class ClipFilters(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f,
    val temperature: Float = 0f,
    val vignette: Float = 0f,
    val blur: Float = 0f,
    val sharpen: Float = 0f,
    val grain: Float = 0f,
    val preset: FilterPreset = FilterPreset.NONE
)

enum class FilterPreset(val displayName: String) {
    NONE("Original"),
    VIVID("Vivid"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    NOIR("Noir"),
    FILM("Film"),
    SUNSET("Sunset"),
    DRAMATIC("Dramatic"),
    RETRO("Retro"),
    BLEACH("Bleach Bypass"),
    CROSS("Cross Process"),
    TEAL_ORANGE("Teal & Orange"),
    CYBERPUNK("Cyberpunk"),
    PASTEL("Pastel Dream")
}

// ─── Platform Presets ────────────────────────────────────────────────

enum class PlatformPreset(
    val displayName: String,
    val width: Int,
    val height: Int,
    val maxDurationSeconds: Int,
    val aspectRatio: String
) {
    TIKTOK("TikTok", 1080, 1920, 180, "9:16"),
    INSTAGRAM_REELS("Instagram Reels", 1080, 1920, 90, "9:16"),
    YOUTUBE_SHORTS("YouTube Shorts", 1080, 1920, 60, "9:16"),
    INSTAGRAM_STORY("Instagram Story", 1080, 1920, 60, "9:16"),
    INSTAGRAM_FEED("Instagram Feed", 1080, 1080, 60, "1:1"),
    TWITTER("Twitter / X", 1280, 720, 140, "16:9"),
    FACEBOOK("Facebook", 1280, 720, 240, "16:9"),
    LINKEDIN("LinkedIn", 1920, 1080, 600, "16:9"),
    PINTEREST("Pinterest", 1080, 1920, 600, "9:16"),
    CUSTOM("Custom", 1080, 1920, 300, "9:16")
}

// ─── Export Settings ─────────────────────────────────────────────────

data class ExportSettings(
    val platform: PlatformPreset = PlatformPreset.TIKTOK,
    val quality: ExportQuality = ExportQuality.HIGH,
    val fps: Int = 30,
    val format: VideoFormat = VideoFormat.MP4,
    val includeCaptions: Boolean = true,
    val watermark: String? = null
)

enum class ExportQuality(val displayName: String, val bitrate: Int) {
    LOW("Low (720p)", 2_000_000),
    MEDIUM("Medium (720p)", 4_000_000),
    HIGH("High (1080p)", 8_000_000),
    ULTRA("Ultra (1080p+)", 15_000_000)
}

enum class VideoFormat(val extension: String, val mimeType: String) {
    MP4("mp4", "video/mp4"),
    MOV("mov", "video/quicktime"),
    WEBM("webm", "video/webm")
}

// ─── Templates ───────────────────────────────────────────────────────

data class Template(
    val id: Long = 0,
    val name: String,
    val category: TemplateCategory,
    val captionStyle: CaptionStyle,
    val description: String,
    val thumbnailUrl: String? = null,
    val isBuiltIn: Boolean = true,
    val isPremium: Boolean = false
)

enum class TemplateCategory(val displayName: String) {
    VIRAL("Viral"),
    PROFESSIONAL("Professional"),
    CREATIVE("Creative"),
    MINIMAL("Minimal"),
    BOLD("Bold & Dynamic"),
    NEON("Neon Glow"),
    CINEMATIC("Cinematic"),
    RETRO("Retro"),
    PLAYFUL("Playful"),
    ELEGANT("Elegant")
}

// ─── Brand Presets ───────────────────────────────────────────────────

data class BrandPreset(
    val id: Long = 0,
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
)

// ─── UI State Models ─────────────────────────────────────────────────

data class EditorState(
    val project: Project? = null,
    val selectedClipIndex: Int = 0,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val processingMessage: String = "",
    val undoStack: List<EditorAction> = emptyList(),
    val redoStack: List<EditorAction> = emptyList(),
    val hasUnsavedChanges: Boolean = false
)

sealed class EditorAction {
    data class TrimClip(val clipId: Long, val oldStart: Long, val oldEnd: Long, val newStart: Long, val newEnd: Long) : EditorAction()
    data class UpdateCaptionStyle(val clipId: Long, val oldStyle: CaptionStyle, val newStyle: CaptionStyle) : EditorAction()
    data class AddTextOverlay(val overlay: TextOverlay) : EditorAction()
    data class RemoveTextOverlay(val overlay: TextOverlay) : EditorAction()
    data class UpdateFilter(val clipId: Long, val oldFilters: ClipFilters, val newFilters: ClipFilters) : EditorAction()
    data class ReorderClips(val oldOrder: List<Long>, val newOrder: List<Long>) : EditorAction()
    data class DeleteClip(val clip: Clip, val position: Int) : EditorAction()
    data class SplitClip(val clipId: Long, val splitPositionMs: Long) : EditorAction()
    data class ChangeSpeed(val clipId: Long, val oldSpeed: Float, val newSpeed: Float) : EditorAction()
}

// ─── Processing State ────────────────────────────────────────────────

sealed class ProcessingState {
    data object Idle : ProcessingState()
    data class Analyzing(val progress: Float = 0f, val message: String = "Analyzing video…") : ProcessingState()
    data class Transcribing(val progress: Float = 0f) : ProcessingState()
    data class DetectingFaces(val progress: Float = 0f) : ProcessingState()
    data class ScoringVirality(val progress: Float = 0f) : ProcessingState()
    data class GeneratingClips(val progress: Float = 0f) : ProcessingState()
    data class ApplyingCaptions(val progress: Float = 0f) : ProcessingState()
    data class Exporting(val progress: Float = 0f, val eta: String = "") : ProcessingState()
    data class Error(val message: String, val throwable: Throwable? = null) : ProcessingState()
    data object Complete : ProcessingState()
}
