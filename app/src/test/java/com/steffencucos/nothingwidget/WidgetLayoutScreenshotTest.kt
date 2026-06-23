package com.steffencucos.nothingwidget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class WidgetLayoutScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:style/Theme.Material.NoActionBar"
    )

    @Test
    fun widgetSolarEventDefaultState() {
        val context = paparazzi.context
        val widthPx = context.dp(260)
        val heightPx = context.dp(110)

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
        }

        LayoutInflater.from(context).inflate(R.layout.widget_solar_event, container, true)

        container.measure(
            ViewGroup.MeasureSpec.makeMeasureSpec(widthPx, ViewGroup.MeasureSpec.EXACTLY),
            ViewGroup.MeasureSpec.makeMeasureSpec(heightPx, ViewGroup.MeasureSpec.EXACTLY)
        )
        container.layout(0, 0, widthPx, heightPx)

        paparazzi.snapshot(container, "widget_solar_event_default")
    }

    private fun android.content.Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
