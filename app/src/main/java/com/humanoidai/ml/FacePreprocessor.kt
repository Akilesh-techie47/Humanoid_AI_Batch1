package com.humanoidai.ml

import android.graphics.*
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.*

/**
 * Shared preprocessing logic for face detection, enrollment, and recognition.
 * Ensures identical image normalization across all vision stages.
 * Part of Phase 4 Week 4 Restoration.
 */
object FacePreprocessor {

    /**
     * Aligns the face based on eye coordinates and applies a circular mask.
     * Aligned faces significantly improve FaceNet recognition accuracy.
     */
    fun alignAndIsolate(
        source: Bitmap,
        face: Face
    ): Bitmap {
        val faceBox = face.boundingBox
        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

        // 1. Calculate Square Crop with Margin (Phase 11 Optimization)
        val side = (max(faceBox.width(), faceBox.height()) * 1.5f).toInt()
        val centerX = faceBox.centerX()
        val centerY = faceBox.centerY()
        
        val left = (centerX - side / 2).coerceAtLeast(0)
        val top = (centerY - side / 2).coerceAtLeast(0)
        val right = (centerX + side / 2).coerceAtMost(source.width)
        val bottom = (centerY + side / 2).coerceAtMost(source.height)
        
        val actualWidth = right - left
        val actualHeight = bottom - top
        
        val initialCrop = Bitmap.createBitmap(source, left, top, actualWidth, actualHeight)
        
        // 2. Alignment (Rotation based on eyes)
        val aligned = if (leftEye != null && rightEye != null) {
            val deltaX = (rightEye.x - leftEye.x).toDouble()
            val deltaY = (rightEye.y - leftEye.y).toDouble()
            val angle = atan2(deltaY, deltaX) * 180.0 / PI
            
            val matrix = Matrix()
            val rotCenterX = (rightEye.x + leftEye.x) / 2f - left
            val rotCenterY = (rightEye.y + leftEye.y) / 2f - top
            matrix.postRotate(angle.toFloat(), rotCenterX, rotCenterY)
            
            Bitmap.createBitmap(initialCrop, 0, 0, initialCrop.width, initialCrop.height, matrix, true)
        } else {
            initialCrop
        }
        
        // 3. Circular Mask (Isolation) - Essential for FaceNet accuracy on mobile
        val size = min(aligned.width, aligned.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        val srcRect = Rect(
            (aligned.width - size) / 2, 
            (aligned.height - size) / 2, 
            (aligned.width + size) / 2, 
            (aligned.height + size) / 2
        )
        val destRect = Rect(0, 0, size, size)
        canvas.drawBitmap(aligned, srcRect, destRect, paint)
        
        // Cleanup intermediate bitmaps immediately
        if (aligned != initialCrop) aligned.recycle()
        if (initialCrop != source) initialCrop.recycle()
        
        return output
    }
}
