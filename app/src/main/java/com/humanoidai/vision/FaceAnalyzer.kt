package com.humanoidai.vision

import android.annotation.SuppressLint
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.humanoidai.ml.*
import kotlin.math.abs

class FaceAnalyzer(
    private val embeddingHelper: FaceEmbeddingHelper,
    private val recognitionManager: FaceRecognitionManager,
    private val enrollmentManager: FaceEnrollmentManager,
    private val contextEngine: com.humanoidai.context.ContextEngine,
    private val onResults: (List<DetectedPerson>) -> Unit,
    private val fisheyeCorrector: FisheyeCorrector? = null,
    var isFrontCamera: Boolean = false,
) : ImageAnalysis.Analyzer {

    // Lazy initialization to prevent constructor crash if native libs fail
    private val frameEnhancer by lazy { FrameEnhancer() }
    private val faceTracker by lazy { FaceTracker() }
    
    // ---- Presence Management (Memory) ----
    private val lastSeenMap = mutableMapOf<String, Long>()
    private val greetingHistory = mutableMapOf<String, Long>()
    private val greetingCooldown = 10 * 60 * 1000L // 10 minutes
    private val presenceTimeout = 8000L           // 8 seconds

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.05f) 
            .build()
    )

    private var frameCount = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        // Recognition interval (Phase 9 Performance Optimization)
        val shouldRecognize = (frameCount % 5 == 0)

        val rotation = imageProxy.imageInfo.rotationDegrees
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Optimization: Pass mediaImage directly to ML Kit (Efficient)
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                // Lazy bitmap creation: only if we have faces AND it's a recognition turn
                var processedBitmap: Bitmap? = null
                
                if (faces.isNotEmpty() && shouldRecognize) {
                    processedBitmap = try {
                        val bitmap = imageProxy.toBitmap()
                        val matrix = Matrix()
                        matrix.postRotate(rotation.toFloat())
                        if (isFrontCamera) matrix.postScale(-1f, 1f)
                        
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        
                        // Apply Fisheye Correction if enabled
                        val corrected = if (FisheyeCorrector.isOpenCVLoaded) {
                            fisheyeCorrector?.correct(rotated) ?: rotated
                        } else rotated
                        
                        if (rotated != bitmap && rotated != corrected) rotated.recycle()
                        if (bitmap != rotated) bitmap.recycle()
                        
                        corrected
                    } catch (e: Exception) {
                        Log.e("FaceAnalyzer", "Bitmap conversion failed: ${e.message}")
                        null
                    }
                }

                val rawResults = processFaces(
                    faces, processedBitmap,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat(),
                    shouldRecognize,
                    rotation
                )
                
                // Temporal Smoothing via FaceTracker
                val trackerInputs = rawResults.map { RawDetection(it.boundingBox, it.name, it.confidence) }
                val trackResults = faceTracker.track(trackerInputs)
                
                val now = System.currentTimeMillis()
                val finalResults = rawResults.mapIndexed { index, person ->
                    val track = trackResults.find { it.trackId == person.trackId || (index < trackResults.size && person.trackId == -1) }
                    val smoothedName = track?.name ?: person.name
                    val smoothedConf = track?.confidence ?: person.confidence
                    
                    val isNew = updatePresence(smoothedName, now)
                    person.copy(
                        name = smoothedName,
                        confidence = smoothedConf,
                        isNewArrival = isNew,
                        trackId = track?.trackId ?: -1
                    )
                }

                // Cleanup presence map
                val iterator = lastSeenMap.entries.iterator()
                while (iterator.hasNext()) {
                    if (now - iterator.next().value > presenceTimeout) {
                        iterator.remove()
                    }
                }

                onResults(finalResults)
                contextEngine.updateFromVision(finalResults)
                
                // Cleanup lazy bitmap
                processedBitmap?.recycle()
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Detection failed: ${e.message}")
            }
            .addOnCompleteListener { 
                imageProxy.close() 
            }
    }

    private fun updatePresence(name: String, now: Long): Boolean {
        if (name == "UNKNOWN") return false
        lastSeenMap[name] = now
        val lastGreeted = greetingHistory[name] ?: 0L
        if (now - lastGreeted > greetingCooldown) {
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
        performRecognition: Boolean,
        rotation: Int
    ): List<DetectedPerson> {
        if (faces.isEmpty()) return emptyList()

        // Handle dimension swap for 90/270 degree rotation
        val isRotated = rotation == 90 || rotation == 270
        val effectiveWidth = if (isRotated) imageHeight else imageWidth
        val effectiveHeight = if (isRotated) imageWidth else imageHeight

        return faces.mapIndexed { index, face ->
            val box = face.boundingBox

            val scaledBox = RectF(
                box.left.toFloat()   / effectiveWidth,
                box.top.toFloat()    / effectiveHeight,
                box.right.toFloat()  / effectiveWidth,
                box.bottom.toFloat() / effectiveHeight
            )

            scaledBox.left   = scaledBox.left.coerceIn(0f, 1f)
            scaledBox.top    = scaledBox.top.coerceIn(0f, 1f)
            scaledBox.right  = scaledBox.right.coerceIn(0f, 1f)
            scaledBox.bottom = scaledBox.bottom.coerceIn(0f, 1f)

            // 1. Quality Gate (Phase 4 Week 4 Restoration)
            val faceArea = scaledBox.width() * scaledBox.height()
            val distanceCategory = when {
                faceArea > 0.15f -> "NEAR"
                faceArea > 0.04f -> "MEDIUM"
                else -> "FAR"
            }
            
            val headRotY = face.headEulerAngleY // Turn
            val headRotX = face.headEulerAngleX // Tilt
            val isGoodAngle = abs(headRotY) < 30f && abs(headRotX) < 30f
            val isGoodSize = faceArea > 0.02f // Skip tiny faces for recognition
            
            val isLookingAtCamera = abs(headRotY) < 15f && abs(headRotX) < 15f

            // 2. Recognition Logic with Quality Gate
            var faceBitmap: Bitmap? = null
            var (name, confidence) = if (performRecognition && fullBitmap != null && isGoodAngle && isGoodSize) {
                try {
                    val faceCrop = FacePreprocessor.alignAndIsolate(fullBitmap, face)
                    faceBitmap = faceCrop
                    val embedding = embeddingHelper.getEmbedding(faceCrop)
                    recognitionManager.findMatch(embedding)
                } catch (e: Exception) {
                    Log.e("FaceAnalyzer", "Recognition failed: ${e.message}")
                    Pair("UNKNOWN", 0f)
                }
            } else {
                // Return a temporary marker. The FaceTracker will handle smoothing.
                Pair("STABLE", -1f) 
            }

            // 3. Liveness and Attention
            val leftEyeOpen = face.leftEyeOpenProbability ?: -1f
            val rightEyeOpen = face.rightEyeOpenProbability ?: -1f
            val isBlinking = if (leftEyeOpen != -1f && rightEyeOpen != -1f) {
                (leftEyeOpen < 0.2f || rightEyeOpen < 0.2f)
            } else false

            val movementScore = (abs(headRotY) + abs(headRotX)) / 40f
            val livenessScore = (if (isBlinking) 0.5f else 0f) + (movementScore * 0.5f).coerceAtMost(0.5f)

            val label = if (name != "UNKNOWN" && name != "ANALYZING" && !name.startsWith("PROBABLE_")) {
                enrollmentManager.getLabel(name)
            } else if (name.startsWith("PROBABLE_")) {
                "PROBABLE"
            } else {
                "VISITOR"
            }

            if (index == 0) {
                val smileProb = face.smilingProbability ?: -1f
                if (smileProb > 0.7f) {
                    contextEngine.updateEmotion(
                        com.humanoidai.context.model.EmotionType.JOY,
                        smileProb,
                        com.humanoidai.context.model.EmotionSource.FACE
                    )
                } else if (smileProb >= 0f) {
                    contextEngine.updateEmotion(
                        com.humanoidai.context.model.EmotionType.NEUTRAL,
                        1f - smileProb,
                        com.humanoidai.context.model.EmotionSource.FACE
                    )
                }
            }

            DetectedPerson(
                boundingBox = scaledBox,
                name        = name,
                confidence  = confidence,
                isPrimary   = index == 0,
                label       = label,
                faceBitmap  = faceBitmap,
                livenessScore = livenessScore,
                distanceCategory = distanceCategory,
                isLookingAtCamera = isLookingAtCamera
            )
        }
    }

    private fun cropFace(bitmap: Bitmap, box: Rect): Bitmap {
        val left   = box.left.coerceAtLeast(0)
        val top    = box.top.coerceAtLeast(0)
        val right  = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        val width  = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun release() {
        try {
            detector.close()
            fisheyeCorrector?.release()
        } catch (e: Exception) {
            android.util.Log.e("FaceAnalyzer", "Release error: ${e.message}")
        }
    }
}
