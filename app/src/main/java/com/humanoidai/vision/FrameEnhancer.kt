package com.humanoidai.vision

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import com.humanoidai.ml.FisheyeCorrector

data class EnhancementConfig(
    val denoise: Boolean = true,
    val sharpen: Boolean = true,
    val clahe: Boolean = true,
    val brightnessThreshold: Double = 50.0 // Threshold for denoising
)

/**
 * Advanced OpenCV-based pre-processing pipeline for camera frames.
 * Improves sharpness, exposure, and noise before AI analysis.
 */
class FrameEnhancer(private val config: EnhancementConfig = EnhancementConfig()) {

    // Lazy initialization to prevent UnsatisfiedLinkError on constructor call
    private val clahe by lazy { 
        if (FisheyeCorrector.isOpenCVLoaded) {
            Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        } else null
    }

    fun enhance(bgrMat: Mat): Mat {
        if (!FisheyeCorrector.isOpenCVLoaded) return bgrMat
        
        var processed = bgrMat.clone()

        try {
            // 1. Auto White Balance (Simple Gray World approximation)
            autoWhiteBalance(processed)

            // 2. Denoising (Conditional on low light)
            if (config.denoise && calculateBrightness(processed) < config.brightnessThreshold) {
                val denoised = Mat()
                Photo.fastNlMeansDenoisingColored(processed, denoised, 3f, 3f, 7, 21)
                processed.release()
                processed = denoised
            }

            // 3. Contrast Enhancement (CLAHE on L channel of Lab)
            if (config.clahe) {
                val localClahe = clahe
                if (localClahe != null) {
                    val lab = Mat()
                    Imgproc.cvtColor(processed, lab, Imgproc.COLOR_BGR2Lab)
                    val channels = mutableListOf<Mat>()
                    Core.split(lab, channels)
                    
                    localClahe.apply(channels[0], channels[0])
                    
                    Core.merge(channels, lab)
                    Imgproc.cvtColor(lab, processed, Imgproc.COLOR_Lab2BGR)
                    
                    lab.release()
                    channels.forEach { it.release() }
                }
            }

            // 4. Sharpening (Unsharp Mask)
            if (config.sharpen) {
                val blurred = Mat()
                Imgproc.GaussianBlur(processed, blurred, Size(0.0, 0.0), 3.0)
                Core.addWeighted(processed, 1.5, blurred, -0.5, 0.0, processed)
                blurred.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("FrameEnhancer", "Enhancement failed: ${e.message}")
            return bgrMat
        }

        return processed
    }

    private fun autoWhiteBalance(mat: Mat) {
        val channels = mutableListOf<Mat>()
        Core.split(mat, channels)
        
        val avg = channels.map { Core.mean(it).`val`[0] }
        val gray = avg.average()
        
        for (i in 0 until 3) {
            val factor = if (avg[i] > 0) gray / avg[i] else 1.0
            channels[i].convertTo(channels[i], -1, factor, 0.0)
        }
        
        Core.merge(channels, mat)
        channels.forEach { it.release() }
    }

    private fun calculateBrightness(mat: Mat): Double {
        val hsv = Mat()
        Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV)
        val channels = mutableListOf<Mat>()
        Core.split(hsv, channels)
        val brightness = Core.mean(channels[2]).`val`[0]
        
        hsv.release()
        channels.forEach { it.release() }
        return brightness
    }

    fun release() {
        // No explicit release needed for most Mat wrappers in this class as they are local,
        // but it's good practice to have it.
    }
}
