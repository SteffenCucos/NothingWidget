package com.steffencucos.nothingwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

object PhaseWatchIconRenderer {
    fun render(sizePx: Int, phase: Float, darkMode: Boolean, accentColor: Int): Bitmap {
        val size = sizePx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val radius = size * 0.39f
        val progress = intervalProgress(phase)

        val brightColor = if (darkMode) Color.WHITE else Color.rgb(17, 17, 17)
        val dimColor = if (darkMode) Color.rgb(112, 112, 112) else Color.rgb(150, 150, 150)
        val horizonColor = if (darkMode) Color.rgb(180, 180, 180) else Color.rgb(90, 90, 90)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = horizonColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.018f
            strokeCap = Paint.Cap.ROUND
        }
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brightColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.025f
            strokeCap = Paint.Cap.ROUND
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }

        val startAngle = 205f
        val sweepAngle = 130f
        val count = if (size >= 96) 46 else 28
        val completed = (count - 1) * progress
        for (i in 0 until count) {
            val t = i / (count - 1f)
            val angle = Math.toRadians((startAngle + sweepAngle * t).toDouble())
            dotPaint.color = if (i <= completed) brightColor else dimColor
            dotPaint.alpha = if (i <= completed) 230 else 145
            val x = center + cos(angle).toFloat() * radius
            val y = center + sin(angle).toFloat() * radius
            canvas.drawCircle(x, y, size * 0.024f, dotPaint)
        }

        val markerAngle = Math.toRadians((startAngle + sweepAngle * progress).toDouble())
        canvas.drawCircle(
            center + cos(markerAngle).toFloat() * radius,
            center + sin(markerAngle).toFloat() * radius,
            size * 0.046f,
            accentPaint
        )

        val horizonY = center + size * 0.17f
        canvas.drawLine(center - size * 0.25f, horizonY, center + size * 0.25f, horizonY, linePaint)
        canvas.drawLine(center - size * 0.17f, horizonY + size * 0.045f, center + size * 0.17f, horizonY + size * 0.045f, linePaint)

        val sunRadius = size * 0.105f
        canvas.drawArc(
            center - sunRadius,
            horizonY - sunRadius,
            center + sunRadius,
            horizonY + sunRadius,
            180f,
            180f,
            false,
            sunPaint
        )

        for (i in 0 until 7) {
            val rayAngle = Math.toRadians((210 + i * 20).toDouble())
            val inner = sunRadius * 1.35f
            val outer = sunRadius * 1.75f
            canvas.drawLine(
                center + cos(rayAngle).toFloat() * inner,
                horizonY + sin(rayAngle).toFloat() * inner,
                center + cos(rayAngle).toFloat() * outer,
                horizonY + sin(rayAngle).toFloat() * outer,
                sunPaint
            )
        }

        return bitmap
    }

    private fun intervalProgress(phase: Float): Float {
        val normalized = ((phase % 1f) + 1f) % 1f
        return if (normalized < 0.5f) normalized * 2f else (normalized - 0.5f) * 2f
    }
}
