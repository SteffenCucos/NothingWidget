package com.steffencucos.nothingwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
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
        appWidgetIds.forEach { widgetId -> updateWidget(context, appWidgetManager, widgetId) }
        scheduleRefreshes(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == SYSTEM_CONFIGURATION_ACTION || intent.action == ACTION_SIMULATION_TICK) {
            refreshAll(context)
        }
    }

    override fun onEnabled(context: Context) {
        scheduleRefreshes(context)
    }

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

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = manager.getAppWidgetIds(android.content.ComponentName(context, SolarEventWidgetProvider::class.java))
            widgetIds.forEach { widgetId -> updateWidget(context, manager, widgetId) }
            scheduleRefreshes(context)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val event = SolarEventRepository(context).getNextEvent(WidgetPreferences.currentWidgetTime(context))
            val style = WidgetPreferences.getStyle(context)
            val accentColor = WidgetPreferences.getAccentColor(context).argb
            val iconSizeDp = if (style == WidgetStyle.NOTHING) 34 else 40
            val iconBitmap = PhaseWatchIconRenderer.render(
                sizePx = dpToPx(context, iconSizeDp),
                phase = phaseFor(event.label, event.progressPercent),
                darkMode = isDarkMode(context),
                accentColor = accentColor
            )
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, widgetLayout(context, style)).apply {
                setTextViewText(R.id.eventStatus, event.statusText.uppercase())
                setImageViewBitmap(R.id.eventIcon, iconBitmap)
                if (style == WidgetStyle.NOTHING) {
                    val dotSizeSp = WidgetPreferences.getDotTextSizeSp(context).toFloat()
                    setTextViewText(R.id.eventLabel, DotMatrixText.render(event.label, maxCharacters = 7))
                    setTextViewText(R.id.eventTime, DotMatrixText.render(event.displayTime, maxCharacters = 7))
                    setTextViewTextSize(R.id.eventLabel, android.util.TypedValue.COMPLEX_UNIT_SP, dotSizeSp)
                    setTextViewTextSize(R.id.eventTime, android.util.TypedValue.COMPLEX_UNIT_SP, dotSizeSp)
                    applyAccentColor(accentColor)
                } else {
                    setTextViewText(R.id.eventLabel, event.label.uppercase())
                    setTextViewText(R.id.eventTime, event.displayTime.uppercase())
                }
                setTextViewText(R.id.eventRemaining, event.timeRemaining)
                setTextViewText(R.id.progressText, "${event.progressPercent}%")
                setProgressBar(R.id.eventProgress, 100, event.progressPercent, false)
                setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun RemoteViews.applyAccentColor(accentColor: Int) {
            setTextColor(R.id.statusAccentDot, accentColor)
            setTextColor(R.id.iconAccentDot, accentColor)
            setTextColor(R.id.progressText, accentColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setColorStateList(R.id.eventProgress, "setProgressTintList", ColorStateList.valueOf(accentColor))
            }
        }

        private fun phaseFor(label: String, progressPercent: Int): Float {
            val progress = progressPercent.coerceIn(0, 100) / 100f
            return if (label.uppercase() == "SUNSET") progress * 0.5f else 0.5f + progress * 0.5f
        }

        private fun isDarkMode(context: Context): Boolean {
            val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return mode == Configuration.UI_MODE_NIGHT_YES
        }

        private fun dpToPx(context: Context, value: Int): Int =
            (value * context.resources.displayMetrics.density).toInt()

        private fun widgetLayout(context: Context, style: WidgetStyle): Int {
            if (style == WidgetStyle.CLASSIC) return R.layout.widget_solar_event
            return if (isDarkMode(context)) R.layout.widget_solar_event_nothing else R.layout.widget_solar_event_nothing_light
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
