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

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val startAngle = 205f
        val sweepAngle = 130f
        val dotCount = if (size >= 96) 58 else 34
        val completed = (dotCount - 1) * p
        val arcDotRadius = size * 0.018f

        for (i in 0 until dotCount) {
            val t = i / (dotCount - 1f)
            val angleDeg = startAngle + sweepAngle * t
            val angle = Math.toRadians(angleDeg.toDouble())
            val x = center + cos(angle).toFloat() * radius
            val y = center + sin(angle).toFloat() * radius
            val active = i <= completed

            drawDot(
                canvas = canvas,
                paint = dotPaint,
                x = x,
                y = y,
                radius = arcDotRadius,
                color = if (active) brightColor else dimColor,
                alpha = if (active) 245 else 105
            )
        }

        drawDottedTicks(
            canvas = canvas,
            paint = dotPaint,
            center = center,
            radius = radius,
            size = size,
            color = railColor
        )

        val markerAngle = Math.toRadians((startAngle + sweepAngle * p).toDouble())
        val markerX = center + cos(markerAngle).toFloat() * radius
        val markerY = center + sin(markerAngle).toFloat() * radius
        val glyphSize = size * 0.20f

        drawDot(
            canvas = canvas,
            paint = dotPaint,
            x = markerX,
            y = markerY,
            radius = glyphSize * 0.64f,
            color = accentColor,
            alpha = 58
        )
        when (body) {
            SolarBody.SUN -> drawDotSun(canvas, dotPaint, markerX, markerY, glyphSize, accentColor, brightColor)
            SolarBody.MOON -> drawDotMoon(canvas, dotPaint, markerX, markerY, glyphSize, accentColor, brightColor)
        }

        drawDottedHorizon(
            canvas = canvas,
            paint = dotPaint,
            center = center,
            horizonY = center + size * 0.18f,
            size = size,
            color = railColor
        )

        return bitmap
    }

    private fun drawDottedTicks(
        canvas: Canvas,
        paint: Paint,
        center: Float,
        radius: Float,
        size: Int,
        color: Int
    ) {
        val tickDotRadius = size * 0.011f
        for (tickAngleDeg in listOf(225f, 270f, 315f)) {
            val angle = Math.toRadians(tickAngleDeg.toDouble())
            for (step in 0 until 3) {
                val tickRadius = radius - size * 0.045f + step * size * 0.025f
                drawDot(
                    canvas = canvas,
                    paint = paint,
                    x = center + cos(angle).toFloat() * tickRadius,
                    y = center + sin(angle).toFloat() * tickRadius,
                    radius = tickDotRadius,
                    color = color,
                    alpha = 145
                )
            }
        }
    }

    private fun drawDottedHorizon(
        canvas: Canvas,
        paint: Paint,
        center: Float,
        horizonY: Float,
        size: Int,
        color: Int
    ) {
        drawDottedRow(canvas, paint, center, horizonY, size * 0.46f, size * 0.028f, size * 0.010f, color, 190)
        drawDottedRow(canvas, paint, center, horizonY + size * 0.045f, size * 0.30f, size * 0.028f, size * 0.010f, color, 150)
    }

    private fun drawDottedRow(
        canvas: Canvas,
        paint: Paint,
        centerX: Float,
        y: Float,
        width: Float,
        spacing: Float,
        radius: Float,
        color: Int,
        alpha: Int
    ) {
        val count = (width / spacing).toInt().coerceAtLeast(2)
        val startX = centerX - spacing * (count - 1) / 2f
        for (i in 0 until count) {
            drawDot(canvas, paint, startX + i * spacing, y, radius, color, alpha)
        }
    }

    private fun drawDotSun(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        size: Float,
        accentColor: Int,
        brightColor: Int
    ) {
        val dotRadius = size * 0.075f

        drawDot(canvas, paint, x, y, dotRadius * 1.25f, brightColor, 255)

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            drawDot(
                canvas = canvas,
                paint = paint,
                x = x + cos(angle).toFloat() * size * 0.23f,
                y = y + sin(angle).toFloat() * size * 0.23f,
                radius = dotRadius,
                color = brightColor,
                alpha = 235
            )
        }

        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            drawDot(
                canvas = canvas,
                paint = paint,
                x = x + cos(angle).toFloat() * size * 0.48f,
                y = y + sin(angle).toFloat() * size * 0.48f,
                radius = dotRadius * 0.82f,
                color = accentColor,
                alpha = 235
            )
        }
    }

    private fun drawDotMoon(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        size: Float,
        accentColor: Int,
        brightColor: Int
    ) {
        val outerRadius = size * 0.43f
        val cutoutRadius = outerRadius * 0.88f
        val cutoutX = x + outerRadius * 0.38f
        val cutoutY = y - outerRadius * 0.06f
        val spacing = size * 0.17f
        val dotRadius = size * 0.06f
        val rows = 5

        for (row in -rows..rows) {
            for (col in -rows..rows) {
                val px = x + col * spacing
                val py = y + row * spacing
                val dx = px - x
                val dy = py - y
                val cutDx = px - cutoutX
                val cutDy = py - cutoutY
                val insideOuter = dx * dx + dy * dy <= outerRadius * outerRadius
                val insideCutout = cutDx * cutDx + cutDy * cutDy <= cutoutRadius * cutoutRadius
                if (insideOuter && !insideCutout) {
                    val color = if (col <= -1) accentColor else brightColor
                    drawDot(canvas, paint, px, py, dotRadius, color, 235)
                }
            }
        }
    }

    private fun drawDot(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        radius: Float,
        color: Int,
        alpha: Int
    ) {
        paint.color = color
        paint.alpha = alpha.coerceIn(0, 255)
        canvas.drawCircle(x, y, radius, paint)
    }

    private fun intervalProgress(phase: Float): Float {
        val normalized = normalizePhase(phase)
        return if (normalized < 0.5f) normalized * 2f else (normalized - 0.5f) * 2f
    }

    private fun normalizePhase(phase: Float): Float = ((phase % 1f) + 1f) % 1f
}
