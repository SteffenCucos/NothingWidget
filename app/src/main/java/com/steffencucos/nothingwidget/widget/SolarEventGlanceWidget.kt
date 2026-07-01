package com.steffencucos.nothingwidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.steffencucos.nothingwidget.MainActivity
import com.steffencucos.nothingwidget.solar.SolarEvent
import com.steffencucos.nothingwidget.solar.SolarEventRepository

class SolarEventGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SolarEventGlanceWidget
}

object SolarEventGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(80.dp, 80.dp),
            DpSize(160.dp, 80.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val event = SolarEventRepository(context).getNextEvent(WidgetPreferences.currentWidgetTime(context))
        val accent = WidgetPreferences.getAccentColorArgb(context)
        val compactIconBitmap = PhaseWatchIconRenderer.render(
            sizePx = 140,
            phase = phaseFor(event),
            darkMode = true,
            accentColor = accent
        )
        val wideIconBitmap = PhaseWatchIconRenderer.render(
            sizePx = 200,
            phase = phaseFor(event),
            darkMode = true,
            accentColor = accent
        )
        val compactTimeBitmap = TimeDotMatrixRenderer.render(
            value = event.displayTime,
            heightPx = 44,
            color = AndroidColor.WHITE
        )
        val wideTimeBitmap = TimeDotMatrixRenderer.render(
            value = event.displayTime,
            heightPx = 56,
            color = AndroidColor.WHITE
        )

        provideContent {
            SolarEventGlanceContent(
                event = event,
                compactIconBitmap = compactIconBitmap,
                wideIconBitmap = wideIconBitmap,
                compactTimeBitmap = compactTimeBitmap,
                wideTimeBitmap = wideTimeBitmap
            )
        }
    }

    private fun phaseFor(event: SolarEvent): Float {
        val progress = event.cycleProgress.coerceIn(0f, 1f)
        return if (event.label.uppercase() == "SUNSET") progress * 0.5f else 0.5f + progress * 0.5f
    }
}

@Composable
private fun SolarEventGlanceContent(
    event: SolarEvent,
    compactIconBitmap: Bitmap,
    wideIconBitmap: Bitmap,
    compactTimeBitmap: Bitmap,
    wideTimeBitmap: Bitmap
) {
    val currentSize = LocalSize.current
    val wide = currentSize.width >= 140.dp
    if (wide) {
        SolarEventWideGlanceContent(event, wideIconBitmap, wideTimeBitmap)
    } else {
        SolarEventCompactGlanceContent(event, compactIconBitmap, compactTimeBitmap)
    }
}

@Composable
private fun SolarEventCompactGlanceContent(
    event: SolarEvent,
    iconBitmap: Bitmap,
    timeBitmap: Bitmap
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.Black))
            .clickable(actionStartActivity<MainActivity>())
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.label.uppercase(),
            style = TextStyle(
                color = ColorProvider(Color(0xFFBDBDBD)),
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Image(
            provider = ImageProvider(timeBitmap),
            contentDescription = null,
            modifier = GlanceModifier.height(20.dp)
        )
        Spacer(modifier = GlanceModifier.height(1.dp))
        Image(
            provider = ImageProvider(iconBitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(34.dp)
        )
    }
}

@Composable
private fun SolarEventWideGlanceContent(
    event: SolarEvent,
    iconBitmap: Bitmap,
    timeBitmap: Bitmap
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.Black))
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier.width(82.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NEXT",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8A8A8A)),
                        fontSize = 7.sp
                    )
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = event.label.uppercase(),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFBDBDBD)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Image(
                provider = ImageProvider(timeBitmap),
                contentDescription = null,
                modifier = GlanceModifier.height(24.dp)
            )
        }
        Spacer(modifier = GlanceModifier.width(4.dp))
        Image(
            provider = ImageProvider(iconBitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(56.dp)
        )
    }
}
