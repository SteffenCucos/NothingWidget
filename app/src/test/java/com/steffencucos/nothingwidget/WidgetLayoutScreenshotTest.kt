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
import com.steffencucos.nothingwidget.widget.DotMatrixText
import com.steffencucos.nothingwidget.widget.PhaseWatchIconRenderer
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
            snapshotName = "widget_solar_event_2x2_clean",
            widthDp = 160,
            heightDp = 160
        ) { container ->
            container.setPhaseIcon(sizeDp = 40, phase = 0.25f, darkMode = false)
        }
    }

    @Test
    fun nothingWidgetDefaultStateFitsTwoByTwo() {
        snapshotWidgetLayout(
            layoutId = R.layout.widget_solar_event_nothing,
            snapshotName = "widget_solar_event_2x2_nothing",
            widthDp = 160,
            heightDp = 160
        ) { container ->
            container.findViewById<TextView>(R.id.eventLabel).text = DotMatrixText.render("SUNSET", maxCharacters = 8)
            container.findViewById<TextView>(R.id.eventTime).text = DotMatrixText.render("7:42 PM", maxCharacters = 8)
            container.setPhaseIcon(sizeDp = 86, phase = 0.38f, darkMode = true)
        }
    }

    @Test
    fun nothingWideWidgetDefaultStateFitsMedium() {
        snapshotWidgetLayout(
            layoutId = R.layout.widget_solar_event_nothing_wide,
            snapshotName = "widget_solar_event_2x4_nothing",
            widthDp = 320,
            heightDp = 160
        ) { container ->
            container.findViewById<TextView>(R.id.eventStatus).text = "NEXT"
            container.findViewById<TextView>(R.id.eventLabel).text = DotMatrixText.render("SUNSET", maxCharacters = 8)
            container.findViewById<TextView>(R.id.eventTime).text = DotMatrixText.render("7:42 PM", maxCharacters = 8)
            container.findViewById<TextView>(R.id.eventRemaining).text = "IN 2H 18M"
            container.setPhaseIcon(sizeDp = 126, phase = 0.38f, darkMode = true)
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
            PhaseWatchIconRenderer.render(
                sizePx = context.dp(sizeDp),
                phase = phase,
                darkMode = darkMode,
                accentColor = Color.rgb(245, 34, 45)
            )
        )
    }

    private fun android.content.Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
