package com.steffencucos.nothingwidget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.steffencucos.nothingwidget.widget.PhaseWatchIconRenderer
import com.steffencucos.nothingwidget.widget.TimeDotMatrixRenderer
import org.junit.Rule
import org.junit.Test

class WidgetLayoutScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:style/Theme.Material.NoActionBar"
    )

    @Test
    fun cleanWidgetDefaultStateFitsTwoByTwo() {
        snapshotWidgetLayout(R.layout.widget_solar_event, "widget_solar_event_2x2_clean", 160, 160) { container ->
            container.setPhaseIcon(sizeDp = 40, phase = 0.25f, darkMode = false)
        }
    }

    @Test
    fun nothingWidgetDefaultStateFitsOneByOne() {
        snapshotWidgetLayout(R.layout.widget_solar_event_nothing, "widget_solar_event_1x1_nothing", 96, 96) { container ->
            container.findViewById<TextView>(R.id.eventLabel).text = "SUNSET"
            container.findViewById<TextView>(R.id.eventRemaining).text = "IN 2H 18M"
            container.setDotTime(heightDp = 24, value = "7:42 PM", color = Color.WHITE)
            container.setPhaseIcon(sizeDp = 64, phase = 0.38f, darkMode = true)
        }
    }

    @Test
    fun nothingWideWidgetDefaultStateFitsOneByTwo() {
        snapshotWidgetLayout(R.layout.widget_solar_event_nothing_wide, "widget_solar_event_1x2_nothing", 180, 96) { container ->
            container.findViewById<TextView>(R.id.eventStatus).text = "NEXT"
            container.findViewById<TextView>(R.id.eventLabel).text = "SUNSET"
            container.findViewById<TextView>(R.id.eventRemaining).text = "IN 2H 18M"
            container.setDotTime(heightDp = 30, value = "7:42 PM", color = Color.WHITE)
            container.setPhaseIcon(sizeDp = 84, phase = 0.38f, darkMode = true)
        }
    }

    private fun snapshotWidgetLayout(
        @LayoutRes layoutId: Int,
        snapshotName: String,
        widthDp: Int,
        heightDp: Int,
        configure: (FrameLayout) -> Unit = {}
    ) {
        val context = paparazzi.context
        val widthPx = context.dp(widthDp)
        val heightPx = context.dp(heightDp)
        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
        }
        LayoutInflater.from(context).inflate(layoutId, container, true)
        configure(container)
        container.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        container.layout(0, 0, widthPx, heightPx)
        paparazzi.snapshot(container, snapshotName)
    }

    private fun FrameLayout.setPhaseIcon(sizeDp: Int, phase: Float, darkMode: Boolean) {
        findViewById<ImageView>(R.id.eventIcon).setImageBitmap(
            PhaseWatchIconRenderer.render(context.dp(sizeDp), phase, darkMode, Color.rgb(245, 34, 45))
        )
    }

    private fun FrameLayout.setDotTime(heightDp: Int, value: String, color: Int) {
        findViewById<ImageView>(R.id.eventTime).setImageBitmap(
            TimeDotMatrixRenderer.render(value, context.dp(heightDp), color)
        )
    }

    private fun android.content.Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
