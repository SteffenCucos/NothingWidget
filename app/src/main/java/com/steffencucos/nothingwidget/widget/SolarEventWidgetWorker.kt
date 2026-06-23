package com.steffencucos.nothingwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.steffencucos.nothingwidget.location.DeviceLocationProvider
import com.steffencucos.nothingwidget.location.LocationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SolarEventWidgetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val location = DeviceLocationProvider(applicationContext).getLastKnownLocationBlocking()
        if (location != null) {
            LocationStore(applicationContext).save(location)
        }

        val manager = AppWidgetManager.getInstance(applicationContext)
        val component = ComponentName(applicationContext, SolarEventWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)

        widgetIds.forEach { widgetId ->
            SolarEventWidgetProvider.updateWidget(applicationContext, manager, widgetId)
        }

        Result.success()
    }
}
