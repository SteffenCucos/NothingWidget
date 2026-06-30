package com.steffencucos.nothingwidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.Text
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
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
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val event = SolarEventRepository(context).getNextEvent(WidgetPreferences.currentWidgetTime(context))
        val accent = WidgetPreferences.getAccentColor(context).argb
        val iconBitmap = PhaseWatchIconRenderer.render(
            sizePx = 280,
            phase = phaseFor(event),
            darkMode = true,
            accentColor = accent
        )
        val timeBitmap = TimeDotMatrixRenderer.render(
            text = event.displayTime,
            heightPx = 76,
            color = AndroidColor.WHITE
        )

        provideContent {
            SolarEventGlanceContent(
                event = event,
                iconBitmap = iconBitmap,
                timeBitmap = timeBitmap,
                accentColor = accent
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
    iconBitmap: Bitmap,
    timeBitmap: Bitmap,
    accentColor: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.Black))
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = event.statusText.uppercase(),
            style = TextStyle(
                color = ColorProvider(Color(0xFFBDBDBD)),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Image(
            provider = ImageProvider(iconBitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(104.dp)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = event.label.uppercase(),
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
        Image(
            provider = ImageProvider(timeBitmap),
            contentDescription = null,
            modifier = GlanceModifier.height(38.dp)
        )
        Text(
            text = "IN ${event.timeRemaining.uppercase().replace(" LEFT", "").trim()}",
            style = TextStyle(
                color = ColorProvider(Color(0xFFE0E0E0)),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = "${event.progressPercent}%",
            style = TextStyle(
                color = ColorProvider(Color(accentColor)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}
