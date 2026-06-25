package com.steffencucos.nothingwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object TimeDotMatrixRenderer {
    private val glyphs = mapOf(
        '0' to arrayOf("111", "101", "101", "101", "101", "101", "111"),
        '1' to arrayOf("010", "110", "010", "010", "010", "010", "111"),
        '2' to arrayOf("111", "001", "001", "111", "100", "100", "111"),
        '3' to arrayOf("111", "001", "001", "111", "001", "001", "111"),
        '4' to arrayOf("101", "101", "101", "111", "001", "001", "001"),
        '5' to arrayOf("111", "100", "100", "111", "001", "001", "111"),
        '6' to arrayOf("111", "100", "100", "111", "101", "101", "111"),
        '7' to arrayOf("111", "001", "001", "010", "010", "010", "010"),
        '8' to arrayOf("111", "101", "101", "111", "101", "101", "111"),
        '9' to arrayOf("111", "101", "101", "111", "001", "001", "111"),
        ':' to arrayOf("0", "1", "1", "0", "1", "1", "0"),
        'A' to arrayOf("111", "101", "101", "111", "101", "101", "101"),
        'P' to arrayOf("111", "101", "101", "111", "100", "100", "100"),
        'M' to arrayOf("101", "111", "111", "101", "101", "101", "101")
    )

    fun render(value: String, heightPx: Int, color: Int = Color.WHITE): Bitmap {
        val normalized = value.uppercase().trim()
        val suffix = when {
            normalized.endsWith(" AM") -> "AM"
            normalized.endsWith(" PM") -> "PM"
            else -> ""
        }
        val main = if (suffix.isBlank()) normalized else normalized.dropLast(3).trim()

        val mainStep = (heightPx / 8f).coerceAtLeast(3f)
        val suffixStep = mainStep * 0.55f
        val mainWidth = measure(main, mainStep)
        val suffixWidth = if (suffix.isBlank()) 0f else measure(suffix, suffixStep) + mainStep
        val width = (mainWidth + suffixWidth + mainStep).toInt().coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(16)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }

        drawText(canvas, main, x = mainStep * 0.5f, y = mainStep * 0.5f, step = mainStep, paint = paint)
        if (suffix.isNotBlank()) {
            val suffixY = height - suffixStep * 7.5f
            drawText(canvas, suffix, x = mainWidth + mainStep, y = suffixY, step = suffixStep, paint = paint)
        }
        return bitmap
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, step: Float, paint: Paint) {
        var cursor = x
        text.forEach { char ->
            if (char == ' ') {
                cursor += step * 2f
                return@forEach
            }
            val glyph = glyphs[char] ?: return@forEach
            glyph.forEachIndexed { row, pattern ->
                pattern.forEachIndexed { column, pixel ->
                    if (pixel == '1') {
                        canvas.drawCircle(cursor + column * step, y + row * step, step * 0.34f, paint)
                    }
                }
            }
            cursor += (glyph.maxOf { it.length } + 1) * step
        }
    }

    private fun measure(text: String, step: Float): Float {
        var width = 0f
        text.forEach { char ->
            width += if (char == ' ') {
                step * 2f
            } else {
                ((glyphs[char]?.maxOf { it.length } ?: 0) + 1) * step
            }
        }
        return width
    }
}
