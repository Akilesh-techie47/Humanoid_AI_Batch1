package com.humanoidai.ml

import android.annotation.SuppressLint
import android.graphics.*
import android.view.Surface
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
    private val onResults: (List<DetectedPerson>) -> Unit,
    var deviceRotation: Int = Surface.ROTATION_0,
    var isFrontCamera: Boolean = false
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.20f)
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

        // Use actual device rotation, not the imageProxy rotation
        // This ensures ML Kit gets correct orientation even when screen is locked
        val rotationDegrees = when (deviceRotation) {
            Surface.ROTATION_0   -> if (isFrontCamera) 270 else 90
            Surface.ROTATION_90  -> 0
            Surface.ROTATION_180 -> if (isFrontCamera) 90 else 270
            Surface.ROTATION_270 -> 180
            else                 -> 90
        }

        val imageWidth  = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val bitmap = imageProxy.toBitmap()
                val results = processFaces(faces, bitmap, imageWidth, imageHeight, rotationDegrees)
                onResults(results)
            }
            .addOnFailureListener { }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun processFaces(
        faces: List<Face>,
        fullBitmap: Bitmap?,
        imageWidth: Float,
        imageHeight: Float,
        rotationDegrees: Int
    ): List<DetectedPerson> {
        if (faces.isEmpty()) return emptyList()

        return faces.mapIndexed { index, face ->
            val box = face.boundingBox

            // Scale bounding box from image coordinates to overlay view coordinates
            // The overlay is drawn over the full preview — we normalize to 0..1 then
            // the overlay view scales to its own pixel dimensions at draw time.
            // Correct — normalize to 0..1 range
// RoiOverlayView will scale to its own pixel dimensions
            val isPortrait = rotationDegrees == 90 || rotationDegrees == 270
            val canvasWidth = if (isPortrait) imageHeight else imageWidth
            val canvasHeight = if (isPortrait) imageWidth else imageHeight

            val scaledBox = RectF(
                box.left.toFloat() / canvasWidth,
                box.top.toFloat() / canvasHeight,
                box.right.toFloat() / canvasWidth,
                box.bottom.toFloat() / canvasHeight
            )

            // Try to get face embedding for recognition
            val (name, confidence) = if (fullBitmap != null) {
                try {
                    val faceCrop = cropFace(fullBitmap, box, rotationDegrees)
                    val embedding = embeddingHelper.getEmbedding(faceCrop)
                    recognitionManager.findMatch(embedding)
                } catch (_: Exception) {
                    Pair("UNKNOWN", 0f)
                }
            } else {
                Pair("UNKNOWN", 0f)
            }

            DetectedPerson(
                boundingBox = if (isFrontCamera) {
                    // Mirror X for front camera normalized coordinates
                    RectF(1f - scaledBox.right, scaledBox.top, 1f - scaledBox.left, scaledBox.bottom)
                } else {
                    scaledBox
                },
                name = name,
                confidence = confidence,
                isPrimary = index == 0  // largest/first face is primary
            )
        }
    }

    private fun cropFace(bitmap: Bitmap, box: Rect, rotationDegrees: Int): Bitmap {
        val left   = box.left.coerceAtLeast(0)
        val top    = box.top.coerceAtLeast(0)
        val right  = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        val width  = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        val crop = Bitmap.createBitmap(bitmap, left, top, width, height)

        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (isFrontCamera) {
            // Flip horizontally to get a canonical (non-mirrored) face
            // This allows recognition to work across both front and back cameras
            matrix.postScale(-1f, 1f)
        }

        return Bitmap.createBitmap(crop, 0, 0, crop.width, crop.height, matrix, true)
    }
}
