package com.steffencucos.nothingwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SolarEventWidgetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(applicationContext, SolarEventWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)

        widgetIds.forEach { widgetId ->
            SolarEventWidgetProvider.updateWidget(applicationContext, manager, widgetId)
        }

        return Result.success()
    }
}
