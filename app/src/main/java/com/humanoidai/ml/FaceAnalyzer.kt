package com.humanoidai.ml

import android.annotation.SuppressLint
import android.graphics.*
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.ui.components.DetectedPerson

// -----------------------------------------------------------------
// FaceAnalyzer
// -----------------------------------------------------------------
// Plugs into CameraX ImageAnalysis use case.
// Each frame → ML Kit detects faces → crops each face bitmap →
// FaceEmbeddingHelper generates embedding → FaceRecognitionManager
// matches against known faces → calls onResults with DetectedPerson list.
// -----------------------------------------------------------------
class FaceAnalyzer(
    private val embeddingHelper: FaceEmbeddingHelper,
    private val recognitionManager: FaceRecognitionManager,
    private val onResults: (List<DetectedPerson>) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.10f)
            .build()
    )

    // Throttle to avoid flooding — process every other frame
    private var frameCount = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        if (frameCount % 2 != 0) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imageWidth = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val bitmap = try { imageProxy.toBitmap() } catch (e: Exception) { null }
                val results = processFaces(faces, bitmap, imageWidth, imageHeight)
                onResults(results)
            }
            .addOnFailureListener {
                // Silent fail — just skip this frame
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processFaces(
        faces: List<Face>,
        fullBitmap: Bitmap?,
        imageWidth: Float,
        imageHeight: Float
    ): List<DetectedPerson> {
        if (faces.isEmpty()) return emptyList()

        return faces.mapIndexed { index, face ->
            val box = face.boundingBox

            // Scale bounding box from image coordinates to overlay view coordinates
            // The overlay is drawn over the full preview — we normalize to 0..1 then
            // the overlay view scales to its own pixel dimensions at draw time.
            val scaledBox = RectF(
                box.left.toFloat(),
                box.top.toFloat(),
                box.right.toFloat(),
                box.bottom.toFloat()
            )

            // Try to get face embedding for recognition
            val (name, confidence) = if (fullBitmap != null) {
                try {
                    val faceCrop = cropFace(fullBitmap, box)
                    val embedding = embeddingHelper.getEmbedding(faceCrop)
                    recognitionManager.findMatch(embedding)
                } catch (e: Exception) {
                    Pair("UNKNOWN", 0f)
                }
            } else {
                Pair("UNKNOWN", 0f)
            }

            DetectedPerson(
                boundingBox = scaledBox,
                name = name,
                confidence = confidence,
                isPrimary = index == 0  // largest/first face is primary
            )
        }
    }

    private fun cropFace(bitmap: Bitmap, box: android.graphics.Rect): Bitmap {
        val left   = box.left.coerceAtLeast(0)
        val top    = box.top.coerceAtLeast(0)
        val right  = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        val width  = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
