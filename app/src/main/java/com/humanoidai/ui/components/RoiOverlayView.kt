package com.humanoidai.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

data class DetectedPerson(
    val boundingBox: RectF,
    val name: String,
    val confidence: Float,
    val isPrimary: Boolean,
    val heatPoints: List<PointF> = emptyList()
)

class RoiOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val persons = mutableListOf<DetectedPerson>()

    // Primary ROI — cyan glowing box
    private val roiPaint = Paint().apply {
        color = Color.parseColor("#00D4C8")
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        isAntiAlias = true
    }

    // Unknown person — red box
    private val unknownPaint = Paint().apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Heatmap fill
    private val heatPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    // Label background
    private val labelBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
    }

    private val labelTextPaint = Paint().apply {
        color = Color.parseColor("#00D4C8")
        textSize = 28f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    private val unknownTextPaint = Paint().apply {
        color = Color.parseColor("#EF4444")
        textSize = 24f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    private val cornerLen = 28f  // length of corner bracket

    fun updatePersons(detected: List<DetectedPerson>) {
        persons.clear()
        persons.addAll(detected)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        persons.forEach { person ->
            val box = person.boundingBox

            if (person.isPrimary) {
                drawHeatmap(canvas, person)
                drawCornerBrackets(canvas, box, roiPaint)
                drawDashedBox(canvas, box)
                drawLabel(canvas, box, "${person.name} · ${(person.confidence * 100).toInt()}%", labelTextPaint)
            } else {
                drawCornerBrackets(canvas, box, unknownPaint)
                drawUnknownFill(canvas, box)
                drawLabel(canvas, box, "UNKNOWN · ALERT", unknownTextPaint)
            }
        }
    }

    private fun drawCornerBrackets(canvas: Canvas, box: RectF, paint: Paint) {
        val c = cornerLen
        // Top-left
        canvas.drawLine(box.left, box.top, box.left + c, box.top, paint)
        canvas.drawLine(box.left, box.top, box.left, box.top + c, paint)
        // Top-right
        canvas.drawLine(box.right, box.top, box.right - c, box.top, paint)
        canvas.drawLine(box.right, box.top, box.right, box.top + c, paint)
        // Bottom-left
        canvas.drawLine(box.left, box.bottom, box.left + c, box.bottom, paint)
        canvas.drawLine(box.left, box.bottom, box.left, box.bottom - c, paint)
        // Bottom-right
        canvas.drawLine(box.right, box.bottom, box.right - c, box.bottom, paint)
        canvas.drawLine(box.right, box.bottom, box.right, box.bottom - c, paint)
    }

    private fun drawDashedBox(canvas: Canvas, box: RectF) {
        val dashedPaint = Paint().apply {
            color = Color.argb(60, 0, 212, 200)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        canvas.drawRect(box, dashedPaint)
    }

    private fun drawHeatmap(canvas: Canvas, person: DetectedPerson) {
        // Generate heatmap zones around detected face center
        val cx = person.boundingBox.centerX()
        val cy = person.boundingBox.top + person.boundingBox.height() * 0.25f

        val zones = listOf(
            Triple(cx, cy, 60f) to 0.35f,
            Triple(cx, cy + 40f, 100f) to 0.20f,
            Triple(cx - 20f, cy + 80f, 140f) to 0.12f,
            Triple(cx + 10f, cy + 120f, 180f) to 0.07f,
        )

        zones.forEach { (zone, alpha) ->
            val (x, y, r) = zone
            val shader = RadialGradient(
                x, y, r,
                Color.argb((alpha * 255).toInt(), 0, 212, 200),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            heatPaint.shader = shader
            canvas.drawCircle(x, y, r, heatPaint)
        }
    }

    private fun drawUnknownFill(canvas: Canvas, box: RectF) {
        val fillPaint = Paint().apply {
            color = Color.argb(30, 239, 68, 68)
        }
        canvas.drawRect(box, fillPaint)
    }

    private fun drawLabel(canvas: Canvas, box: RectF, text: String, textPaint: Paint) {
        val padding = 6f
        val textWidth = textPaint.measureText(text)
        val labelRect = RectF(
            box.left, box.top - 36f,
            box.left + textWidth + padding * 2, box.top
        )
        canvas.drawRect(labelRect, labelBgPaint)
        canvas.drawText(text, box.left + padding, box.top - 10f, textPaint)
    }
}