package com.steffencucos.nothingwidget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.steffencucos.nothingwidget.widget.DotMatrixText
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
        snapshotWidgetLayout(
            layoutId = R.layout.widget_solar_event,
            snapshotName = "widget_solar_event_2x2_clean"
        )
    }

    @Test
    fun nothingWidgetDefaultStateFitsTwoByTwo() {
        snapshotWidgetLayout(
            layoutId = R.layout.widget_solar_event_nothing,
            snapshotName = "widget_solar_event_2x2_nothing"
        ) { container ->
            container.findViewById<TextView>(R.id.eventLabel).text = DotMatrixText.render("SUNRISE")
            container.findViewById<TextView>(R.id.eventTime).text = DotMatrixText.render("5:36 AM")
        }
    }

    private fun snapshotWidgetLayout(
        @LayoutRes layoutId: Int,
        snapshotName: String,
        configure: (FrameLayout) -> Unit = {}
    ) {
        val context = paparazzi.context
        val widthPx = context.dp(160)
        val heightPx = context.dp(160)

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

    private fun android.content.Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
