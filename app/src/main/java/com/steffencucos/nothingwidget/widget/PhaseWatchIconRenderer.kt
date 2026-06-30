package com.steffencucos.nothingwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.steffencucos.nothingwidget.solar.SolarBody
import kotlin.math.cos
import kotlin.math.sin

object PhaseWatchIconRenderer {
    fun render(sizePx: Int, phase: Float, darkMode: Boolean, accentColor: Int): Bitmap {
        val normalized = normalizePhase(phase)
        val body = if (normalized < 0.5f) SolarBody.SUN else SolarBody.MOON
        return render(
            sizePx = sizePx,
            body = body,
            progress = intervalProgress(normalized),
            darkMode = darkMode,
            accentColor = accentColor
        )
    }

    fun render(
        sizePx: Int,
        body: SolarBody,
        progress: Float,
        darkMode: Boolean,
        accentColor: Int
    ): Bitmap {
        val size = sizePx.coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val radius = size * 0.39f
        val p = progress.coerceIn(0f, 1f)

        val brightColor = if (darkMode) Color.WHITE else Color.rgb(17, 17, 17)
        val dimColor = if (darkMode) Color.rgb(94, 94, 94) else Color.rgb(172, 172, 172)
        val railColor = if (darkMode) Color.rgb(160, 160, 160) else Color.rgb(92, 92, 92)
        val backgroundColor = if (darkMode) Color.BLACK else Color.WHITE

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = railColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.012f
            strokeCap = Paint.Cap.ROUND
            alpha = 170
        }
        val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = railColor
            style = Paint.Style.STROKE
            strokeWidth = size * 0.016f
            strokeCap = Paint.Cap.ROUND
            alpha = 210
        }
        val markerHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
            alpha = 70
        }

        val startAngle = 205f
        val sweepAngle = 130f
        val dotCount = if (size >= 96) 54 else 32
        val completed = (dotCount - 1) * p
        val dotRadius = size * 0.017f

        for (i in 0 until dotCount) {
            val t = i / (dotCount - 1f)
            val angleDeg = startAngle + sweepAngle * t
            val angle = Math.toRadians(angleDeg.toDouble())
            val x = center + cos(angle).toFloat() * radius
            val y = center + sin(angle).toFloat() * radius
            val active = i <= completed

            dotPaint.color = if (active) brightColor else dimColor
            dotPaint.alpha = if (active) 235 else 115
            canvas.drawCircle(x, y, dotRadius, dotPaint)
        }

        for (tickAngleDeg in listOf(225f, 270f, 315f)) {
            val angle = Math.toRadians(tickAngleDeg.toDouble())
            val inner = radius - size * 0.055f
            val outer = radius + size * 0.015f
            canvas.drawLine(
                center + cos(angle).toFloat() * inner,
                center + sin(angle).toFloat() * inner,
                center + cos(angle).toFloat() * outer,
                center + sin(angle).toFloat() * outer,
                tickPaint
            )
        }

        val markerAngle = Math.toRadians((startAngle + sweepAngle * p).toDouble())
        val markerX = center + cos(markerAngle).toFloat() * radius
        val markerY = center + sin(markerAngle).toFloat() * radius
        val glyphSize = size * 0.18f

        canvas.drawCircle(markerX, markerY, glyphSize * 0.58f, markerHaloPaint)
        when (body) {
            SolarBody.SUN -> drawSun(canvas, markerX, markerY, glyphSize, brightColor)
            SolarBody.MOON -> drawMoon(canvas, markerX, markerY, glyphSize, brightColor, backgroundColor)
        }

        val horizonY = center + size * 0.18f
        canvas.drawLine(center - size * 0.27f, horizonY, center + size * 0.27f, horizonY, horizonPaint)
        canvas.drawLine(center - size * 0.18f, horizonY + size * 0.046f, center + size * 0.18f, horizonY + size * 0.046f, horizonPaint)

        return bitmap
    }

    private fun drawSun(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = size * 0.095f
            strokeCap = Paint.Cap.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val coreRadius = size * 0.23f
        val rayInner = size * 0.39f
        val rayOuter = size * 0.58f

        canvas.drawCircle(x, y, coreRadius, stroke)
        canvas.drawCircle(x, y, coreRadius * 0.45f, fill)

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            canvas.drawLine(
                x + cos(angle).toFloat() * rayInner,
                y + sin(angle).toFloat() * rayInner,
                x + cos(angle).toFloat() * rayOuter,
                y + sin(angle).toFloat() * rayOuter,
                stroke
            )
        }
    }

    private fun drawMoon(canvas: Canvas, x: Float, y: Float, size: Float, color: Int, backgroundColor: Int) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val mask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = backgroundColor
            style = Paint.Style.FILL
        }
        val radius = size * 0.42f

        canvas.drawCircle(x, y, radius, fill)
        canvas.drawCircle(x + radius * 0.38f, y - radius * 0.08f, radius * 0.92f, mask)
    }

    private fun intervalProgress(phase: Float): Float {
        val normalized = normalizePhase(phase)
        return if (normalized < 0.5f) normalized * 2f else (normalized - 0.5f) * 2f
    }

    private fun normalizePhase(phase: Float): Float = ((phase % 1f) + 1f) % 1f
}
