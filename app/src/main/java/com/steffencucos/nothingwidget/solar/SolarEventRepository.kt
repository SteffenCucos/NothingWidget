package com.steffencucos.nothingwidget.solar

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class SolarEventRepository {
    private val formatter = DateTimeFormatter.ofPattern("h:mm a")

    /**
     * Temporary deterministic data source.
     *
     * Next iteration:
     * - read coarse location from Android location APIs
     * - calculate local sunrise/sunset for the current date
     * - persist the last known location and solar event pair
     */
    fun getNextEvent(now: LocalTime = LocalTime.now()): SolarEvent {
        val sunrise = LocalTime.of(6, 0)
        val sunset = LocalTime.of(20, 45)

        return when {
            now.isBefore(sunrise) -> SolarEvent(
                label = "Sunrise",
                displayTime = sunrise.format(formatter),
                progressPercent = progressBetween(LocalTime.MIDNIGHT, sunrise, now)
            )

            now.isBefore(sunset) -> SolarEvent(
                label = "Sunset",
                displayTime = sunset.format(formatter),
                progressPercent = progressBetween(sunrise, sunset, now)
            )

            else -> SolarEvent(
                label = "Sunrise",
                displayTime = "Tomorrow ${sunrise.format(formatter)}",
                progressPercent = progressBetween(sunset, LocalTime.MAX, now)
            )
        }
    }

    private fun progressBetween(start: LocalTime, end: LocalTime, now: LocalTime): Int {
        val total = end.toSecondOfDay() - start.toSecondOfDay()
        val elapsed = now.toSecondOfDay() - start.toSecondOfDay()

        if (total <= 0) return 0

        return ((elapsed.toDouble() / total.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }
}
