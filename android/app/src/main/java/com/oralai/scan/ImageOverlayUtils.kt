package com.oralai.scan

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ImageOverlayUtils {

    /**
     * Draws irregular dashed contours on a bitmap.
     * @param cx Normalized center X (0.0 to 1.0)
     * @param cy Normalized center Y (0.0 to 1.0)
     * @param radius Normalized radius
     */
    fun drawAnalysisOutlines(
        srcBitmap: Bitmap, 
        cx: Float? = null, 
        cy: Float? = null, 
        radius: Float? = null
    ): Bitmap {
        val mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val w = mutableBitmap.width.toFloat()
        val h = mutableBitmap.height.toFloat()

        // Use provided coordinates or default to a slightly offset center
        val centerX = (cx ?: 0.5f) * w
        val centerY = (cy ?: 0.48f) * h
        val baseRadius = (radius ?: 0.12f) * w.coerceAtMost(h)

        // 1. Blue Dashed Outer Safety Boundary (#2196F3) - Thinner
        val outerPaint = Paint().apply {
            color = Color.parseColor("#2196F3")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.005f // Reduced from 0.012f
            pathEffect = DashPathEffect(floatArrayOf(w * 0.015f, w * 0.01f), 0f)
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val outerPath = createIrregularContour(centerX, centerY, baseRadius * 1.5f, segments = 14, randomness = 0.12f)
        canvas.drawPath(outerPath, outerPaint)

        // 2. Yellow Dashed Inner Lesion Margin (#FFEB3B) - Thinner
        val innerPaint = Paint().apply {
            color = Color.parseColor("#FFEB3B")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.007f // Reduced from 0.015f
            pathEffect = DashPathEffect(floatArrayOf(w * 0.01f, w * 0.008f), 0f)
            isAntiAlias = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val innerPath = createIrregularContour(centerX, centerY, baseRadius, segments = 12, randomness = 0.20f)
        canvas.drawPath(innerPath, innerPaint)

        return mutableBitmap
    }

    private fun createIrregularContour(
        cx: Float,
        cy: Float,
        radius: Float,
        segments: Int,
        randomness: Float
    ): Path {
        val path = Path()
        val points = mutableListOf<PointF>()
        val random = Random(42)

        for (i in 0 until segments) {
            val angle = (2 * Math.PI * i / segments).toFloat()
            val r = radius * (1 + (random.nextFloat() - 0.5f) * 2 * randomness)
            val x = cx + r * cos(angle.toDouble()).toFloat()
            val y = cy + r * sin(angle.toDouble()).toFloat()
            points.add(PointF(x, y))
        }

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val p1 = points[i]
                val p0 = points[i - 1]
                val midX = (p0.x + p1.x) / 2
                val midY = (p0.y + p1.y) / 2
                if (i == 1) {
                    path.lineTo(midX, midY)
                } else {
                    path.quadTo(p0.x, p0.y, midX, midY)
                }
            }
            val last = points.last()
            val first = points.first()
            val midX = (last.x + first.x) / 2
            val midY = (last.y + first.y) / 2
            path.quadTo(last.x, last.y, midX, midY)
            path.quadTo(first.x, first.y, points[0].x, points[0].y)
        }
        return path
    }
}
