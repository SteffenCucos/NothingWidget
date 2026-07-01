package com.steffencucos.nothingwidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ColorWheelView(context: Context) : View(context) {
    var onColorChanged: ((Int) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val markerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.BLACK
        alpha = 170
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.rgb(96, 96, 96)
        alpha = 190
    }

    private var wheelBitmap: Bitmap? = null
    private var wheelRadius = 0f
    private var centerX = 0f
    private var centerY = 0f
    private var selectedColor = WidgetAccentColor.RED.argb
    private val selectedHsv = FloatArray(3)

    init {
        Color.colorToHSV(selectedColor, selectedHsv)
        isClickable = true
    }

    fun setSelectedColor(color: Int, notify: Boolean = false) {
        selectedColor = color or 0xFF000000.toInt()
        Color.colorToHSV(selectedColor, selectedHsv)
        invalidate()
        if (notify) onColorChanged?.invoke(selectedColor)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        centerX = width / 2f
        centerY = height / 2f
        wheelRadius = min(width, height) * 0.43f
        wheelBitmap = createWheelBitmap(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        wheelBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawCircle(centerX, centerY, wheelRadius, outlinePaint)

        val hueRadians = Math.toRadians(selectedHsv[0].toDouble())
        val markerDistance = wheelRadius * selectedHsv[1].coerceIn(0f, 1f)
        val markerX = centerX + cos(hueRadians).toFloat() * markerDistance
        val markerY = centerY + sin(hueRadians).toFloat() * markerDistance
        canvas.drawCircle(markerX, markerY, 10f, markerShadowPaint)
        canvas.drawCircle(markerX, markerY, 10f, markerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN && event.action != MotionEvent.ACTION_MOVE) return super.onTouchEvent(event)
        parent?.requestDisallowInterceptTouchEvent(true)
        selectColorAt(event.x, event.y)
        return true
    }

    private fun selectColorAt(x: Float, y: Float) {
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx * dx + dy * dy).coerceAtMost(wheelRadius)
        val saturation = if (wheelRadius == 0f) 0f else distance / wheelRadius
        val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
        setSelectedColor(Color.HSVToColor(floatArrayOf(hue, saturation, 1f)), notify = true)
    }

    private fun createWheelBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                val index = y * width + x
                if (distance <= wheelRadius) {
                    val saturation = distance / wheelRadius
                    val hue = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 360.0) % 360.0).toFloat()
                    pixels[index] = Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
                } else {
                    pixels[index] = Color.TRANSPARENT
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
