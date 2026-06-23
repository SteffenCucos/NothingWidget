package com.steffencucos.nothingwidget.location

import android.content.Context
import android.location.Location

data class StoredLocation(
    val latitude: Double,
    val longitude: Double
)

class LocationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "nothing_widget_location",
        Context.MODE_PRIVATE
    )

    fun save(location: Location) {
        save(location.latitude, location.longitude)
    }

    fun save(latitude: Double, longitude: Double) {
        preferences.edit()
            .putFloat(KEY_LATITUDE, latitude.toFloat())
            .putFloat(KEY_LONGITUDE, longitude.toFloat())
            .apply()
    }

    fun get(): StoredLocation? {
        if (!preferences.contains(KEY_LATITUDE) || !preferences.contains(KEY_LONGITUDE)) {
            return null
        }

        return StoredLocation(
            latitude = preferences.getFloat(KEY_LATITUDE, 0f).toDouble(),
            longitude = preferences.getFloat(KEY_LONGITUDE, 0f).toDouble()
        )
    }

    companion object {
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }
}
