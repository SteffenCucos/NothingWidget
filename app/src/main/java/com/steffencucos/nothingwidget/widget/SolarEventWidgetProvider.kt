package com.steffencucos.nothingwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.steffencucos.nothingwidget.MainActivity
import com.steffencucos.nothingwidget.R
import com.steffencucos.nothingwidget.solar.SolarEventRepository
import java.time.Duration

class SolarEventWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
        schedulePeriodicRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == SYSTEM_CONFIGURATION_ACTION) {
            refreshAll(context)
        }
    }

    override fun onEnabled(context: Context) {
        schedulePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "solar-event-widget-refresh"
        private const val SYSTEM_CONFIGURATION_ACTION = "android.intent.action.CONFIGURATION_CHANGED"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = manager.getAppWidgetIds(
                android.content.ComponentName(context, SolarEventWidgetProvider::class.java)
            )
            widgetIds.forEach { widgetId ->
                updateWidget(context, manager, widgetId)
            }
            schedulePeriodicRefresh(context)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val event = SolarEventRepository(context).getNextEvent()
            val style = WidgetPreferences.getStyle(context)
            val layoutId = widgetLayout(context, style)
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, layoutId).apply {
                setTextViewText(R.id.eventStatus, event.statusText.uppercase())
                if (style == WidgetStyle.NOTHING) {
                    setTextViewText(R.id.eventLabel, DotMatrixText.render(event.label, maxCharacters = 7))
                    setTextViewText(R.id.eventTime, DotMatrixText.render(event.displayTime, maxCharacters = 7))
                } else {
                    setTextViewText(R.id.eventLabel, event.label.uppercase())
                    setTextViewText(R.id.eventTime, event.displayTime.uppercase())
                }
                setTextViewText(R.id.eventRemaining, event.timeRemaining)
                setTextViewText(R.id.eventIcon, event.iconText)
                setTextViewText(R.id.progressText, "${event.progressPercent}%")
                setProgressBar(R.id.eventProgress, 100, event.progressPercent, false)
                setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun widgetLayout(context: Context, style: WidgetStyle): Int {
            if (style == WidgetStyle.CLASSIC) return R.layout.widget_solar_event
            val mode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            return if (mode == android.content.res.Configuration.UI_MODE_NIGHT_YES) R.layout.widget_solar_event_nothing else R.layout.widget_solar_event_nothing_light
        }

        private fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<SolarEventWidgetWorker>(
                Duration.ofMinutes(30)
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
