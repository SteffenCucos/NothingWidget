package com.steffencucos.nothingwidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.steffencucos.nothingwidget.R
import com.steffencucos.nothingwidget.solar.SolarEventRepository
import java.time.Duration
import java.util.concurrent.TimeUnit

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

    override fun onEnabled(context: Context) {
        schedulePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "solar-event-widget-refresh"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repository = SolarEventRepository()
            val event = repository.getNextEvent()

            val views = RemoteViews(context.packageName, R.layout.widget_solar_event).apply {
                setTextViewText(R.id.eventLabel, event.label)
                setTextViewText(R.id.eventTime, event.displayTime)
                setTextViewText(R.id.progressText, "${event.progressPercent}%")
                setProgressBar(R.id.eventProgress, 100, event.progressPercent, false)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
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
