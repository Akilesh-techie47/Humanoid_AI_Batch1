package com.humanoidai.vision

import android.graphics.PointF
import android.graphics.RectF

data class DetectedPerson(
    val boundingBox: RectF,
    val name: String,
    val confidence: Float,
    val isPrimary: Boolean,
    val label: String = "Unknown",
    val faceBitmap: android.graphics.Bitmap? = null,
    val heatPoints: List<PointF> = emptyList(),
    val isNewArrival: Boolean = false,
    val trackId: Int = -1,
    val livenessScore: Float = 0f,
    val distanceCategory: String = "MEDIUM", // NEAR, MID, FAR
    val isLookingAtCamera: Boolean = false
)
