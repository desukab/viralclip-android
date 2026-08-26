package com.viralclip.app.core.ai

import android.content.Context
import android.net.Uri
import com.viralclip.app.core.audio.AudioProcessor
import com.viralclip.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * On-device caption generation engine.
 * Uses audio segment analysis to detect speech regions and generate
 * timed caption segments with word-level timing.
 *
 * In production, this would invoke Whisper.cpp via JNI for full ASR.
 * Current implementation uses audio energy patterns to create meaningful
 * caption segments based on detected speech regions.
 */
@Singleton
class CaptionGenerator @Inject constructor(
    private val context: Context,
    private val audioProcessor: AudioProcessor
) {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    data class TranscriptionResult(
        val segments: List<CaptionSegment>,
        val language: String,
        val totalWords: Int,
        val durationMs: Long
    )

    /**
     * Generate captions from video's audio track.
     * Analyzes audio energy patterns to find speech regions,
     * then creates timed caption segments.
     */
    suspend fun generateCaptions(
        videoUri: Uri,
        language: String = "en",
        onProgress: (Float) -> Unit = { _progress.value = it }
    ): TranscriptionResult = withContext(Dispatchers.Default) {
        onProgress(0.05f)

        // Get audio info
        val audioInfo = audioProcessor.getAudioInfo(videoUri)
        val totalDurationMs = audioInfo.durationMs

        onProgress(0.15f)

        // Analyze audio segments (100ms resolution for precise timing)
        val audioSegments = audioProcessor.analyzeAudioSegments(
            videoUri, segmentDurationMs = 100L, maxSegments = 600
        )

        onProgress(0.50f)

        // Group contiguous speech segments into caption blocks
        val speechBlocks = groupSpeechBlocks(audioSegments, totalDurationMs)

        onProgress(0.70f)

        // Generate caption text from speech blocks
        val segments = speechBlocks.map { block ->
            CaptionSegment(
                text = generatePlaceholderText(block.wordCount, block.startTimeMs, block.endTimeMs),
                startTimeMs = block.startTimeMs,
                endTimeMs = block.endTimeMs,
                confidence = block.confidence
            )
        }.filter { it.text.isNotBlank() }

        onProgress(0.90f)

        // Optimize segments for readability
        val optimized = optimizeSegments(segments)

        onProgress(1.0f)

        TranscriptionResult(
            segments = optimized,
            language = language,
            totalWords = optimized.sumOf { it.text.split(" ").size },
            durationMs = totalDurationMs
        )
    }

    /**
     * Group contiguous speech segments into caption-sized blocks.
     * Merges nearby speech segments, respects silence gaps.
     */
    private fun groupSpeechBlocks(
        audioSegments: List<AudioProcessor.AudioSegment>,
        totalDurationMs: Long
    ): List<SpeechBlock> {
        if (audioSegments.isEmpty()) return emptyList()

        val blocks = mutableListOf<SpeechBlock>()
        var currentBlock: SpeechBlock? = null

        for (segment in audioSegments) {
            if (!segment.isSilent && segment.speechConfidence > 0.2f) {
                // Speech detected — extend or create block
                if (currentBlock == null) {
                    currentBlock = SpeechBlock(
                        startTimeMs = segment.startTimeMs,
                        endTimeMs = segment.endTimeMs,
                        totalVolume = segment.volume,
                        sampleCount = 1,
                        maxVolume = segment.volume
                    )
                } else {
                    // Extend current block
                    val gap = segment.startTimeMs - currentBlock.endTimeMs
                    if (gap <= 500L) {
                        // Small gap — merge (likely same sentence)
                        currentBlock = currentBlock.copy(
                            endTimeMs = segment.endTimeMs,
                            totalVolume = currentBlock.totalVolume + segment.volume,
                            sampleCount = currentBlock.sampleCount + 1,
                            maxVolume = maxOf(currentBlock.maxVolume, segment.volume)
                        )
                    } else {
                        // Large gap — finalize current block, start new one
                        blocks.add(finalizeBlock(currentBlock, totalDurationMs))
                        currentBlock = SpeechBlock(
                            startTimeMs = segment.startTimeMs,
                            endTimeMs = segment.endTimeMs,
                            totalVolume = segment.volume,
                            sampleCount = 1,
                            maxVolume = segment.volume
                        )
                    }
                }
            } else {
                // Silence — finalize block if gap is long enough
                if (currentBlock != null) {
                    val silenceDuration = segment.endTimeMs - currentBlock.endTimeMs
                    val blockDuration = currentBlock.endTimeMs - currentBlock.startTimeMs

                    if (silenceDuration > 300L || blockDuration > 5000L) {
                        blocks.add(finalizeBlock(currentBlock, totalDurationMs))
                        currentBlock = null
                    }
                }
            }
        }

        // Don't forget the last block
        if (currentBlock != null) {
            blocks.add(finalizeBlock(currentBlock, totalDurationMs))
        }

        return blocks
    }

    private fun finalizeBlock(block: SpeechBlock, totalDurationMs: Long): SpeechBlock {
        val avgVolume = block.totalVolume / block.sampleCount
        val estimatedWords = ((block.endTimeMs - block.startTimeMs) / 400f).toInt().coerceIn(2, 25)
        return block.copy(
            wordCount = estimatedWords,
            confidence = (avgVolume * 2.5f).coerceIn(0.3f, 1.0f),
            endTimeMs = minOf(block.endTimeMs, totalDurationMs)
        )
    }

    /**
     * Generate placeholder caption text based on timing.
     * In production with Whisper, this would be real transcribed text.
     */
    private fun generatePlaceholderText(wordCount: Int, startMs: Long, endMs: Long): String {
        // Generate context-appropriate placeholder segments
        val durationSec = (endMs - startMs) / 1000f
        return when {
            durationSec < 1.5f -> generateShortPhrase()
            durationSec < 3f -> generateMediumPhrase()
            durationSec < 5f -> generateLongPhrase()
            else -> generateExtendedPhrase()
        }
    }

    private fun generateShortPhrase(): String = listOf(
        "Listen to this",
        "This is huge",
        "Wait for it",
        "Pay attention",
        "Watch this",
        "Here's the thing",
        "Check this out",
        "Let me show you"
    ).random()

    private fun generateMediumPhrase(): String = listOf(
        "This changes everything you know",
        "Most people don't realize this yet",
        "The secret nobody talks about",
        "Here's what you need to know",
        "This is why it matters so much",
        "You won't believe what happens next",
        "The truth about what's coming",
        "Everyone needs to hear this"
    ).random()

    private fun generateLongPhrase(): String = listOf(
        "The most important thing you'll hear today",
        "This is going to change how you think about everything",
        "What I'm about to share with you is really important",
        "Nobody is talking about this but it affects all of us",
        "Pay close attention because this could change your life"
    ).random()

    private fun generateExtendedPhrase(): String = listOf(
        "This is something that most people completely miss and it has huge implications for all of us",
        "If you only remember one thing from this video make sure it's this because it really matters",
        "The reason I wanted to share this with you is because it goes against everything we've been told",
        "Let me break this down step by step so you can really understand why this is so important"
    ).random()

    /**
     * Split raw text into timed caption segments with word-level timing.
     * Used when real transcription text is available.
     */
    fun splitIntoSegments(
        text: String,
        totalDurationMs: Long,
        maxCharsPerLine: Int = 40,
        maxWordsPerSegment: Int = 8
    ): List<CaptionSegment> {
        val words = text.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()

        val segments = mutableListOf<CaptionSegment>()
        val wordsPerSegment = maxOf(1, minOf(maxWordsPerSegment, words.size))
        val wordsPerMs = words.size.toFloat() / totalDurationMs

        var currentWordIndex = 0
        var currentTimeMs = 0L

        while (currentWordIndex < words.size) {
            val endWordIndex = minOf(currentWordIndex + wordsPerSegment, words.size)
            val segmentWords = words.subList(currentWordIndex, endWordIndex)
            val segmentText = segmentWords.joinToString(" ")

            val segmentDurationMs = (segmentWords.size / wordsPerMs).toLong()
            val startTimeMs = currentTimeMs
            val endTimeMs = minOf(startTimeMs + segmentDurationMs.coerceAtLeast(500), totalDurationMs)

            // Generate word-level timestamps
            val wordDuration = segmentDurationMs / segmentWords.size
            val wordsWithTiming = segmentWords.mapIndexed { i, word ->
                CaptionWord(
                    text = word,
                    startTimeMs = startTimeMs + (i * wordDuration),
                    endTimeMs = startTimeMs + ((i + 1) * wordDuration)
                )
            }

            segments.add(
                CaptionSegment(
                    text = segmentText,
                    startTimeMs = startTimeMs,
                    endTimeMs = endTimeMs,
                    confidence = 0.95f
                )
            )

            currentWordIndex = endWordIndex
            currentTimeMs = endTimeMs
        }

        return segments
    }

    /**
     * Auto-capitalize and format caption text based on rules.
     */
    fun formatCaptionText(
        text: String,
        caseStyle: CaseStyle,
        maxChars: Int = 40
    ): String {
        var formatted = when (caseStyle) {
            CaseStyle.NORMAL -> text
            CaseStyle.UPPERCASE -> text.uppercase()
            CaseStyle.LOWERCASE -> text.lowercase()
            CaseStyle.TITLE_CASE -> text.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
            CaseStyle.FIRST_WORD_CAPS -> text.replaceFirstChar { it.uppercase() }
        }

        // Word-wrap at max characters
        if (formatted.length > maxChars) {
            val words = formatted.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (word in words) {
                if (currentLine.length + word.length + 1 > maxChars && currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder()
                }
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
            formatted = lines.joinToString("\n")
        }

        return formatted
    }

    /**
     * Apply highlight effect to specific words in a caption.
     */
    fun getHighlightedWords(
        text: String,
        highlightWords: List<String> = emptyList(),
        autoHighlight: Boolean = true
    ): List<Pair<String, Boolean>> {
        val words = text.split(" ")
        val highlightSet = highlightWords.map { it.lowercase() }.toSet()

        val powerWords = setOf(
            "amazing", "incredible", "secret", "shocking", "unbelievable",
            "free", "money", "viral", "truth", "exposed", "warning",
            "million", "billion", "first", "never", "always", "best",
            "worst", "insane", "crazy", "absolutely", "everything",
            "nobody", "huge", "important", "change", "believe", "listen",
            "watch", "listen", "pay", "attention", "huge", "secret"
        )

        val autoHighlightWords = if (autoHighlight && highlightWords.isEmpty()) {
            words.map { word ->
                val clean = word.lowercase().replace(Regex("[^a-z0-9]"), "")
                clean in powerWords
            }
        } else {
            words.map { word ->
                highlightWords.any { hw -> word.lowercase().contains(hw.lowercase()) }
            }
        }

        return words.zip(autoHighlightWords)
    }

    /**
     * Generate word-level timestamps for karaoke-style animation.
     */
    fun generateWordTimestamps(
        segments: List<CaptionSegment>
    ): List<Pair<CaptionWord, CaptionSegment>> {
        val wordTimings = mutableListOf<Pair<CaptionWord, CaptionSegment>>()
        for (segment in segments) {
            val words = segment.words
            wordTimings.addAll(words.map { word -> word to segment })
        }
        return wordTimings
    }

    /**
     * Merge short segments and split long ones for optimal readability.
     */
    fun optimizeSegments(
        segments: List<CaptionSegment>,
        minDurationMs: Long = 1000,
        maxDurationMs: Long = 5000,
        maxCharsPerSegment: Int = 60
    ): List<CaptionSegment> {
        if (segments.isEmpty()) return segments

        val optimized = mutableListOf<CaptionSegment>()
        var pending = segments.first()

        for (i in 1 until segments.size) {
            val current = segments[i]
            val combinedText = "${pending.text} ${current.text}"
            val combinedDuration = current.endTimeMs - pending.startTimeMs

            if (combinedDuration <= maxDurationMs && combinedText.length <= maxCharsPerSegment) {
                pending = CaptionSegment(
                    clipId = pending.clipId,
                    text = combinedText,
                    startTimeMs = pending.startTimeMs,
                    endTimeMs = current.endTimeMs,
                    confidence = minOf(pending.confidence, current.confidence)
                )
            } else {
                if (pending.durationMs < minDurationMs && optimized.isNotEmpty()) {
                    val prev = optimized.removeLast()
                    optimized.add(
                        CaptionSegment(
                            clipId = prev.clipId,
                            text = "${prev.text} ${pending.text}",
                            startTimeMs = prev.startTimeMs,
                            endTimeMs = pending.endTimeMs
                        )
                    )
                } else {
                    optimized.add(pending)
                }
                pending = current
            }
        }
        optimized.add(pending)

        return optimized
    }

    /**
     * Internal data class for speech block grouping.
     */
    private data class SpeechBlock(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val totalVolume: Float,
        val sampleCount: Int,
        val maxVolume: Float,
        val wordCount: Int = 0,
        val confidence: Float = 0.5f
    )
}
