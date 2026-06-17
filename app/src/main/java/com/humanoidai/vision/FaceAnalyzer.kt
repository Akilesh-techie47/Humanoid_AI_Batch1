package com.humanoidai.vision

import android.annotation.SuppressLint
import android.graphics.*
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.ml.*

class FaceAnalyzer(
    private val embeddingHelper: FaceEmbeddingHelper,
    private val recognitionManager: FaceRecognitionManager,
    private val enrollmentManager: FaceEnrollmentManager,
    private val onResults: (List<DetectedPerson>) -> Unit,
    private val fisheyeCorrector: FisheyeCorrector? = null,
    var deviceRotation: Int = Surface.ROTATION_0,
    var isFrontCamera: Boolean = false
) : ImageAnalysis.Analyzer {

    // ---- Presence Management (Memory) ----
    private val lastSeenMap = mutableMapOf<String, Long>()
    private val greetingHistory = mutableMapOf<String, Long>()
    private val GREETING_COOLDOWN = 10 * 60 * 1000L // 10 minutes
    private val PRESENCE_TIMEOUT = 8000L           // 8 seconds

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.05f) 
            .build()
    )

    private var frameCount = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        // Throttle for stability on table
        if (frameCount % 3 != 0) {
            imageProxy.close()
            return
        }

        val bitmap = try {
            val original = imageProxy.toBitmap()
            if (isFrontCamera) {
                val matrix = Matrix()
                matrix.postScale(-1f, 1f)
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            } else {
                original
            }
        } catch (e: Exception) {
            imageProxy.close()
            return
        }

        val correctedBitmap = if (fisheyeCorrector != null && FisheyeCorrector.isOpenCVLoaded) {
            try {
                fisheyeCorrector.correct(bitmap)
            } catch (e: Exception) {
                bitmap
            }
        } else {
            bitmap
        }

        val inputImage = InputImage.fromBitmap(correctedBitmap, 0)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val rawResults = processFaces(
                    faces, correctedBitmap,
                    correctedBitmap.width.toFloat(),
                    correctedBitmap.height.toFloat(),
                    0
                )
                
                val now = System.currentTimeMillis()
                val finalResults = rawResults.map { person ->
                    val isNew = updatePresence(person.name, now)
                    person.copy(isNewArrival = isNew)
                }

                // Cleanup gone people (Android 10+ compatible removal)
                val iterator = lastSeenMap.entries.iterator()
                while (iterator.hasNext()) {
                    if (now - iterator.next().value > PRESENCE_TIMEOUT) {
                        iterator.remove()
                    }
                }

                onResults(finalResults)
            }
            .addOnFailureListener { }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun updatePresence(name: String, now: Long): Boolean {
        lastSeenMap[name] = now
        val lastGreeted = greetingHistory[name] ?: 0L
        if (now - lastGreeted > GREETING_COOLDOWN) {
            greetingHistory[name] = now
            return true
        }
        return false
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

            val scaledBox = RectF(
                box.left.toFloat()   / imageWidth,
                box.top.toFloat()    / imageHeight,
                box.right.toFloat()  / imageWidth,
                box.bottom.toFloat() / imageHeight
            )

            scaledBox.left   = scaledBox.left.coerceIn(0f, 1f)
            scaledBox.top    = scaledBox.top.coerceIn(0f, 1f)
            scaledBox.right  = scaledBox.right.coerceIn(0f, 1f)
            scaledBox.bottom = scaledBox.bottom.coerceIn(0f, 1f)

            var faceBitmap: Bitmap? = null
            val (name, confidence) = if (fullBitmap != null) {
                try {
                    val faceCrop = cropFace(fullBitmap, box)
                    faceBitmap = faceCrop
                    val embedding = embeddingHelper.getEmbedding(faceCrop)
                    recognitionManager.findMatch(embedding)
                } catch (e: Exception) {
                    Pair("UNKNOWN", 0f)
                }
            } else {
                Pair("UNKNOWN", 0f)
            }

            val label = if (name != "UNKNOWN") {
                enrollmentManager.getLabel(name)
            } else {
                "VISITOR"
            }

            DetectedPerson(
                boundingBox = scaledBox,
                name        = name,
                confidence  = confidence,
                isPrimary   = index == 0,
                label       = label,
                faceBitmap  = faceBitmap
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
