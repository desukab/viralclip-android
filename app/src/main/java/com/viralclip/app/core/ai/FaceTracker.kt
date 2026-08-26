package com.viralclip.app.core.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.viralclip.app.domain.model.FacePosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered face detection and tracking for smart video reframing.
 * Uses Google ML Kit for on-device face detection.
 * Supports speaker tracking, center-framing, and multi-face layouts.
 */
@Singleton
class FaceTracker @Inject constructor(
    private val context: Context
) {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
        FaceDetection.getClient(options)
    }

    data class FaceTrackResult(
        val frames: List<TrackedFrame>,
        val dominantSpeaker: FacePosition?,
        val avgFaceSize: Float,
        val facePresentRatio: Float
    )

    data class TrackedFrame(
        val timestampMs: Long,
        val faces: List<FacePosition>,
        val mainFace: FacePosition?,
        val refocusPoint: RefocusPoint
    )

    data class RefocusPoint(
        val x: Float,   // 0.0 - 1.0 normalized
        val y: Float    // 0.0 - 1.0 normalized
    )

    /**
     * Track faces across video frames for smart reframing.
     */
    suspend fun trackFaces(
        frames: List<Pair<Long, Bitmap>>,
        targetWidth: Int = 1080,
        targetHeight: Int = 1920
    ): FaceTrackResult = withContext(Dispatchers.Default) {
        _progress.value = 0f
        val trackedFrames = mutableListOf<TrackedFrame>()
        val allFaces = mutableListOf<FacePosition>()

        for ((index, pair) in frames.withIndex()) {
            val (timestampMs, bitmap) = pair

            try {
                // Create a copy to avoid recycled bitmap issues with ML Kit
                val copy = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height)

                val faces = detectFaces(copy)
                allFaces.addAll(faces)

                val mainFace = selectMainFace(faces)
                val refocusPoint = calculateRefocusPoint(
                    mainFace, faces, targetWidth, targetHeight
                )

                trackedFrames.add(
                    TrackedFrame(
                        timestampMs = timestampMs,
                        faces = faces,
                        mainFace = mainFace,
                        refocusPoint = refocusPoint
                    )
                )
            } catch (_: Exception) {
                // Bitmap may already be recycled or invalid, skip this frame
            }
            _progress.value = (index + 1).toFloat() / frames.size
        }

        val facePresentRatio = if (trackedFrames.isNotEmpty()) {
            trackedFrames.count { it.faces.isNotEmpty() }.toFloat() / trackedFrames.size
        } else 0f

        val avgFaceSize = if (allFaces.isNotEmpty()) {
            allFaces.map { it.width * it.height }.average().toFloat()
        } else 0f

        // Determine dominant speaker (face that appears most frequently)
        val dominantSpeaker = findDominantSpeaker(allFaces)

        _progress.value = 1f

        FaceTrackResult(
            frames = trackedFrames,
            dominantSpeaker = dominantSpeaker,
            avgFaceSize = avgFaceSize,
            facePresentRatio = facePresentRatio
        )
    }

    /**
     * Detect faces in a single bitmap using ML Kit.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<FacePosition> = withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@withContext emptyList()
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            faces.map { face ->
                val bounds = face.boundingBox
                FacePosition(
                    centerX = bounds.centerX().toFloat() / bitmap.width,
                    centerY = bounds.centerY().toFloat() / bitmap.height,
                    width = bounds.width().toFloat() / bitmap.width,
                    height = bounds.height().toFloat() / bitmap.height,
                    confidence = 0.95f
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Calculate optimal crop point for vertical video reframing.
     * Centers on the main face with smart padding.
     */
    fun calculateReframePoint(
        faces: List<FacePosition>,
        sourceWidth: Int,
        sourceHeight: Int,
        targetAspect: Float = 9f / 16f
    ): Pair<Float, Float> {
        if (faces.isEmpty()) {
            // Center of frame when no face detected
            return Pair(0.5f, 0.5f)
        }

        // Use weighted average of face positions
        val totalConfidence = faces.sumOf { it.confidence.toDouble() }.toFloat()
        val weightedX = faces.sumOf { (it.centerX * it.confidence).toDouble() }.toFloat() / totalConfidence
        val weightedY = faces.sumOf { (it.centerY * it.confidence).toDouble() }.toFloat() / totalConfidence

        // Ensure crop doesn't go out of bounds
        val cropWidth = targetAspect * sourceHeight.toFloat() / sourceWidth
        val maxCropX = 1f - cropWidth / 2
        val cropX = weightedX.coerceIn(cropWidth / 2, maxCropX)
        val cropY = weightedY.coerceIn(0.1f, 0.9f)

        return Pair(cropX, cropY)
    }

    private fun selectMainFace(faces: List<FacePosition>): FacePosition? {
        if (faces.isEmpty()) return null
        // Select face closest to center, weighted by size
        return faces.maxByOrNull { face ->
            val centerDist = Math.sqrt(
                ((face.centerX - 0.5) * (face.centerX - 0.5) +
                (face.centerY - 0.4) * (face.centerY - 0.4)).toDouble()
            ).toFloat()
            val sizeScore = face.width * face.height
            sizeScore - centerDist * 0.3f
        }
    }

    private fun calculateRefocusPoint(
        mainFace: FacePosition?,
        allFaces: List<FacePosition>,
        targetWidth: Int,
        targetHeight: Int
    ): RefocusPoint {
        val face = mainFace ?: return RefocusPoint(0.5f, 0.4f)

        // Position face in upper third of vertical frame
        val targetY = 0.35f // Face center at 35% from top
        val offsetX = (face.centerX - 0.5f) * 0.5f // Reduce horizontal offset

        return RefocusPoint(
            x = (face.centerX + offsetX).coerceIn(0.2f, 0.8f),
            y = targetY
        )
    }

    private fun findDominantSpeaker(faces: List<FacePosition>): FacePosition? {
        if (faces.isEmpty()) return null
        // Simple heuristic: most common face position = dominant speaker
        // In production, would use face embedding similarity
        return faces.groupBy {
            Pair((it.centerX * 10).toInt(), (it.centerY * 10).toInt())
        }.maxByOrNull { it.value.size }?.value?.first()
    }

    fun close() {
        detector.close()
    }
}
