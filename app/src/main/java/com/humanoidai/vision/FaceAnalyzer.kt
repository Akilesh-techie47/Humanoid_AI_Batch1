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
    private val contextEngine: com.humanoidai.context.ContextEngine,
    private val onResults: (List<DetectedPerson>) -> Unit,
    private val fisheyeCorrector: FisheyeCorrector? = null,
    var deviceRotation: Int = Surface.ROTATION_0,
    var isFrontCamera: Boolean = false
) : ImageAnalysis.Analyzer {

    // Lazy initialization to prevent constructor crash if native libs fail
    private val frameEnhancer by lazy { FrameEnhancer() }
    private val faceTracker by lazy { FaceTracker() }
    
    // ---- Presence Management (Memory) ----
    private val lastSeenMap = mutableMapOf<String, Long>()
    private val greetingHistory = mutableMapOf<String, Long>()
    private val GREETING_COOLDOWN = 10 * 60 * 1000L // 10 minutes
    private val PRESENCE_TIMEOUT = 8000L           // 8 seconds

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
        // Only run full embedding extraction every 5 frames to save CPU/Battery
        val shouldRecognize = frameCount % 5 == 0

        val rotation = imageProxy.imageInfo.rotationDegrees
        
        val originalBitmap = try {
            val bitmap = imageProxy.toBitmap()
            val matrix = Matrix()
            
            // Handle Rotation
            matrix.postRotate(rotation.toFloat())
            
            // Handle Mirroring for Front Camera
            if (isFrontCamera) {
                matrix.postScale(-1f, 1f)
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            imageProxy.close()
            return
        }

        val processedBitmap = if (FisheyeCorrector.isOpenCVLoaded) {
            try {
                // 1. Fisheye Correction
                val corrected = fisheyeCorrector?.correct(originalBitmap) ?: originalBitmap
                
                // 2. Enhancement Pipeline
                val mat = org.opencv.core.Mat()
                org.opencv.android.Utils.bitmapToMat(corrected, mat)
                
                val bgr = org.opencv.core.Mat()
                org.opencv.imgproc.Imgproc.cvtColor(mat, bgr, org.opencv.imgproc.Imgproc.COLOR_RGBA2BGR)
                
                val enhancedBgr = frameEnhancer.enhance(bgr)
                
                val outputRgba = org.opencv.core.Mat()
                org.opencv.imgproc.Imgproc.cvtColor(enhancedBgr, outputRgba, org.opencv.imgproc.Imgproc.COLOR_BGR2RGBA)
                
                val outputBitmap = Bitmap.createBitmap(outputRgba.cols(), outputRgba.rows(), Bitmap.Config.ARGB_8888)
                org.opencv.android.Utils.matToBitmap(outputRgba, outputBitmap)
                
                mat.release()
                bgr.release()
                enhancedBgr.release()
                outputRgba.release()
                
                outputBitmap
            } catch (e: Exception) {
                android.util.Log.e("FaceAnalyzer", "Vision Pipeline Error: ${e.localizedMessage}")
                originalBitmap 
            }
        } else {
            originalBitmap
        }

        val inputImage = InputImage.fromBitmap(processedBitmap, 0)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                android.util.Log.d("FaceAnalyzer", "Detected ${faces.size} faces")
                val rawResults = processFaces(
                    faces, processedBitmap,
                    processedBitmap.width.toFloat(),
                    processedBitmap.height.toFloat(),
                    shouldRecognize
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

                val iterator = lastSeenMap.entries.iterator()
                while (iterator.hasNext()) {
                    if (now - iterator.next().value > PRESENCE_TIMEOUT) {
                        iterator.remove()
                    }
                }

                onResults(finalResults)
                contextEngine.updateFromVision(finalResults)
            }
            .addOnFailureListener { }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun updatePresence(name: String, now: Long): Boolean {
        if (name == "UNKNOWN") return false
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
        performRecognition: Boolean
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

            // 1. Fisheye Edge Weighting
            // Faces near the edge (0.0 - 0.2 or 0.8 - 1.0) get a confidence penalty
            val centerX = scaledBox.centerX()
            val centerY = scaledBox.centerY()
            val edgeDistance = minOf(centerX, 1f - centerX, centerY, 1f - centerY)
            // Reduced penalty for edge detections to improve Agent-level continuity
            val edgeWeight = (edgeDistance / 0.15f).coerceIn(0.75f, 1.0f)

            // 2. Liveness Check (Blink/Movement)
            val leftEyeOpen = face.leftEyeOpenProbability ?: -1f
            val rightEyeOpen = face.rightEyeOpenProbability ?: -1f
            val isBlinking = if (leftEyeOpen != -1f && rightEyeOpen != -1f) {
                (leftEyeOpen < 0.2f || rightEyeOpen < 0.2f)
            } else false
            
            val headRotY = face.headEulerAngleY // Turn
            val headRotX = face.headEulerAngleX // Tilt
            
            // 3. Distance Estimation
            val faceArea = scaledBox.width() * scaledBox.height()
            val distanceCategory = when {
                faceArea > 0.15f -> "NEAR"
                faceArea > 0.04f -> "MEDIUM"
                else -> "FAR"
            }

            // 4. Attention Detection (Looking at Camera)
            val isLookingAtCamera = Math.abs(headRotY) < 15f && Math.abs(headRotX) < 15f

            val movementScore = (Math.abs(headRotY) + Math.abs(headRotX)) / 40f
            val livenessScore = (if (isBlinking) 0.5f else 0f) + (movementScore * 0.5f).coerceAtMost(0.5f)

            var faceBitmap: Bitmap? = null
            var (name, confidence) = if (performRecognition && fullBitmap != null) {
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

            // Apply Edge Weighting to confidence
            confidence *= edgeWeight

            val label = if (name != "UNKNOWN" && !name.startsWith("PROBABLE_")) {
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
