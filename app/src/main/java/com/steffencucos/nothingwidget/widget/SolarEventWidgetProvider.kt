package com.steffencucos.nothingwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.steffencucos.nothingwidget.MainActivity
import com.steffencucos.nothingwidget.R
import com.steffencucos.nothingwidget.solar.SolarEventRepository
import java.time.Duration

class SolarEventWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
        scheduleRefreshes(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == SYSTEM_CONFIGURATION_ACTION || intent.action == ACTION_SIMULATION_TICK) refreshAll(context)
    }

    override fun onEnabled(context: Context) = scheduleRefreshes(context)

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        cancelSimulationRefresh(context)
    }

    companion object {
        private const val WORK_NAME = "solar-event-widget-refresh"
        private const val SYSTEM_CONFIGURATION_ACTION = "android.intent.action.CONFIGURATION_CHANGED"
        private const val ACTION_SIMULATION_TICK = "com.steffencucos.nothingwidget.action.SIMULATION_TICK"
        private const val SIMULATION_REFRESH_INTERVAL_MS = 15_000L
        private const val SIMULATION_ALARM_REQUEST_CODE = 60
        private const val WIDE_MIN_WIDTH_DP = 100

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, SolarEventWidgetProvider::class.java))
            ids.forEach { updateWidget(context, manager, it) }
            scheduleRefreshes(context)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val event = SolarEventRepository(context).getNextEvent(WidgetPreferences.currentWidgetTime(context))
            val style = WidgetPreferences.getStyle(context)
            val wide = style == WidgetStyle.NOTHING && appWidgetManager.getAppWidgetOptions(appWidgetId).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) >= WIDE_MIN_WIDTH_DP
            val dark = isDarkMode(context)
            val accent = WidgetPreferences.getAccentColor(context).argb
            val iconDp = if (style != WidgetStyle.NOTHING) 40 else if (wide) 84 else 64
            val icon = PhaseWatchIconRenderer.render(dpToPx(context, iconDp), phaseFor(event.label, event.progressPercent), dark, accent)
            val pendingIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val views = RemoteViews(context.packageName, widgetLayout(context, style, wide)).apply {
                setTextViewText(R.id.eventStatus, if (style == WidgetStyle.NOTHING && wide) "NEXT" else event.statusText.uppercase())
                setImageViewBitmap(R.id.eventIcon, icon)
                if (style == WidgetStyle.NOTHING) {
                    val textColor = if (dark) -1 else -15658735
                    val timeHeight = if (wide) 30 else 24
                    setTextViewText(R.id.eventLabel, event.label.uppercase())
                    setTextViewTextSize(R.id.eventLabel, android.util.TypedValue.COMPLEX_UNIT_SP, if (wide) 11f else 9f)
                    setImageViewBitmap(R.id.eventTime, TimeDotMatrixRenderer.render(event.displayTime, dpToPx(context, timeHeight), textColor))
                    setTextViewText(R.id.eventRemaining, remainingText(event.timeRemaining))
                    applyAccentColor(accent)
                } else {
                    setTextViewText(R.id.eventLabel, event.label.uppercase())
                    setTextViewText(R.id.eventTime, event.displayTime.uppercase())
                    setTextViewText(R.id.eventRemaining, event.timeRemaining)
                    setProgressBar(R.id.eventProgress, 100, event.progressPercent, false)
                }
                setTextViewText(R.id.progressText, "${event.progressPercent}%")
                setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun RemoteViews.applyAccentColor(accentColor: Int) {
            setTextColor(R.id.statusAccentDot, accentColor)
            setTextColor(R.id.iconAccentDot, accentColor)
            setTextColor(R.id.progressText, accentColor)
        }

        private fun remainingText(value: String): String = "IN ${value.uppercase().replace(" LEFT", "").trim()}"

        private fun phaseFor(label: String, progressPercent: Int): Float {
            val p = progressPercent.coerceIn(0, 100) / 100f
            return if (label.uppercase() == "SUNSET") p * 0.5f else 0.5f + p * 0.5f
        }

        private fun isDarkMode(context: Context): Boolean = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        private fun dpToPx(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

        private fun widgetLayout(context: Context, style: WidgetStyle, wide: Boolean): Int {
            if (style == WidgetStyle.CLASSIC) return R.layout.widget_solar_event
            val dark = isDarkMode(context)
            return when {
                wide && dark -> R.layout.widget_solar_event_nothing_wide
                wide -> R.layout.widget_solar_event_nothing_wide_light
                dark -> R.layout.widget_solar_event_nothing
                else -> R.layout.widget_solar_event_nothing_light
            }
        }

        private fun scheduleRefreshes(context: Context) {
            schedulePeriodicRefresh(context)
            scheduleSimulationRefresh(context)
        }

        private fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<SolarEventWidgetWorker>(Duration.ofMinutes(30)).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        private fun scheduleSimulationRefresh(context: Context) {
            if (!WidgetPreferences.isTimeSimulationEnabled(context)) {
                cancelSimulationRefresh(context)
                return
            }
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SIMULATION_REFRESH_INTERVAL_MS, simulationPendingIntent(context))
        }

        private fun cancelSimulationRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(simulationPendingIntent(context))
        }

        private fun simulationPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SolarEventWidgetProvider::class.java).apply { action = ACTION_SIMULATION_TICK }
            return PendingIntent.getBroadcast(context, SIMULATION_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
