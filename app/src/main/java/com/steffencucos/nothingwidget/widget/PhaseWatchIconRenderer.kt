package com.steffencucos.nothingwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object PhaseWatchIconRenderer {
    fun render(sizePx: Int, phase: Float, darkMode: Boolean, accentColor: Int): Bitmap {
        val size = sizePx.coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val outerRadius = size * 0.43f
        val innerRadius = size * 0.26f
        val angle = ((phase % 1f) * 360f) - 90f
        val radians = Math.toRadians(angle.toDouble())

        val ringColor = if (darkMode) Color.rgb(106, 106, 106) else Color.rgb(118, 118, 118)
        val faceColor = if (darkMode) Color.rgb(21, 21, 21) else Color.WHITE
        val brightColor = if (darkMode) Color.WHITE else Color.rgb(17, 17, 17)
        val dimColor = if (darkMode) Color.rgb(42, 42, 42) else Color.rgb(216, 216, 216)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = faceColor }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.055f
        }
        val bright = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = brightColor }
        val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dimColor }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }

        canvas.drawCircle(center, center, outerRadius, fill)
        canvas.drawCircle(center, center, outerRadius, ring)

        val disc = RectF(center - innerRadius, center - innerRadius, center + innerRadius, center + innerRadius)
        canvas.drawCircle(center, center, innerRadius, dim)
        canvas.drawArc(disc, angle - 90f, 180f, true, bright)

        val markerRadius = outerRadius - size * 0.02f
        val markerX = center + cos(radians).toFloat() * markerRadius
        val markerY = center + sin(radians).toFloat() * markerRadius
        canvas.drawCircle(center, center, size * 0.018f, accent)
        canvas.drawCircle(markerX, markerY, size * 0.046f, accent)

        return bitmap
    }
}
