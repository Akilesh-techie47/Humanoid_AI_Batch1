package com.humanoidai.ml

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.calib3d.Calib3d
import org.opencv.imgproc.Imgproc

// -----------------------------------------------------------------
// FisheyeCorrector
// -----------------------------------------------------------------
// Corrects barrel distortion from a 180° fisheye lens using
// OpenCV's fisheye undistortion model.
// -----------------------------------------------------------------
class FisheyeCorrector {

    companion object {
        var isOpenCVLoaded = false

        fun initOpenCV(): Boolean {
            if (!isOpenCVLoaded) {
                isOpenCVLoaded = OpenCVLoader.initLocal()
                android.util.Log.d("FisheyeCorrector", "OpenCV loaded: $isOpenCVLoaded")
            }
            return isOpenCVLoaded
        }
    }

    // Fisheye distortion coefficients (D)
    // Refined for typical 180-degree mobile lenses
    private val k1 = 0.08
    private val k2 = -0.15
    private val k3 = 0.02
    private val k4 = 0.00

    // Cached matrices — built once, reused every frame
    private var mapX: Mat? = null
    private var mapY: Mat? = null
    private var lastSize: Size? = null

    // -----------------------------------------------------------------
    // Main correction function
    // -----------------------------------------------------------------
    fun correct(input: Bitmap): Bitmap {
        if (!isOpenCVLoaded) return input

        val inputMat = Mat()
        Utils.bitmapToMat(input, inputMat)

        // Convert RGBA → BGR for OpenCV processing
        val bgrMat = Mat()
        Imgproc.cvtColor(inputMat, bgrMat, Imgproc.COLOR_RGBA2BGR)

        val frameSize = Size(bgrMat.cols().toDouble(), bgrMat.rows().toDouble())

        // Build undistortion maps only when frame size changes
        if (mapX == null || lastSize != frameSize) {
            buildUndistortMaps(frameSize)
            lastSize = frameSize
        }

        // Apply undistortion
        val undistorted = Mat()
        Imgproc.remap(bgrMat, undistorted, mapX, mapY, Imgproc.INTER_LINEAR)

        // Convert back BGR → RGBA
        val outputMat = Mat()
        Imgproc.cvtColor(undistorted, outputMat, Imgproc.COLOR_BGR2RGBA)

        // Convert back to Bitmap
        val output = Bitmap.createBitmap(
            outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(outputMat, output)

        // Release mats
        inputMat.release()
        bgrMat.release()
        undistorted.release()
        outputMat.release()

        return output
    }

    // -----------------------------------------------------------------
    // Build remap matrices for the fisheye undistortion
    // -----------------------------------------------------------------
    private fun buildUndistortMaps(frameSize: Size) {
        // focal length estimation
        val dynamicFx = frameSize.width * 0.42
        val dynamicFy = frameSize.width * 0.42
        val dynamicCx = frameSize.width / 2.0
        val dynamicCy = frameSize.height / 2.0

        val K = Mat(3, 3, CvType.CV_64FC1)
        K.put(0, 0,
            dynamicFx, 0.0, dynamicCx,
            0.0, dynamicFy, dynamicCy,
            0.0, 0.0, 1.0
        )

        // Build distortion coefficients D
        val D = Mat(4, 1, CvType.CV_64FC1)
        D.put(0, 0, k1, k2, k3, k4)

        // balance=0.0 preserves the entire FOV (Full room visibility)
        val newK = Mat()
        Calib3d.fisheye_estimateNewCameraMatrixForUndistortRectify(
            K, D, frameSize, Mat(), newK, 0.0, frameSize
        )

        // Compute remap matrices
        val mx = Mat()
        val my = Mat()
        Calib3d.fisheye_initUndistortRectifyMap(
            K, D, Mat(), newK, frameSize,
            CvType.CV_16SC2, mx, my
        )

        mapX?.release()
        mapY?.release()
        mapX = mx
        mapY = my

        K.release()
        D.release()
        newK.release()
    }

    fun release() {
        mapX?.release()
        mapY?.release()
        mapX = null
        mapY = null
    }
}
