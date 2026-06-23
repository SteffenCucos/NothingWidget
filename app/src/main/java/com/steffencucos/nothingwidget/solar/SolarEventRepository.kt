package com.steffencucos.nothingwidget.solar

import android.content.Context
import com.steffencucos.nothingwidget.location.LocationStore
import com.steffencucos.nothingwidget.location.StoredLocation
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class SolarEventRepository(context: Context) {
    private val locationStore = LocationStore(context)
    private val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun getNextEvent(now: ZonedDateTime = ZonedDateTime.now()): SolarEvent {
        val location = locationStore.get()
            ?: return SolarEvent(
                label = "Location",
                displayTime = "--:--",
                timeRemaining = "Open app to enable",
                progressPercent = 0,
                iconText = "◎",
                statusText = "Waiting for location"
            )

        return getNextEvent(location, now)
    }

    private fun getNextEvent(location: StoredLocation, now: ZonedDateTime): SolarEvent {
        val zoneId = ZoneId.systemDefault()
        val today = now.withZoneSameInstant(zoneId).toLocalDate()
        val events = (-1L..2L)
            .flatMap { offset ->
                val date = today.plusDays(offset)
                listOfNotNull(
                    SolarCalculator.sunrise(date, location.latitude, location.longitude, zoneId)
                        ?.let { TimedSolarEvent("Sunrise", it, "☀") },
                    SolarCalculator.sunset(date, location.latitude, location.longitude, zoneId)
                        ?.let { TimedSolarEvent("Sunset", it, "◐") }
                )
            }
            .sortedBy { it.time }

        if (events.isEmpty()) {
            return SolarEvent(
                label = "Solar event",
                displayTime = "--:--",
                timeRemaining = "Unavailable here",
                progressPercent = 0,
                iconText = "◎",
                statusText = "No event today"
            )
        }

        val localNow = now.withZoneSameInstant(zoneId)
        val nextEvent = events.firstOrNull { it.time.isAfter(localNow) } ?: events.last()
        val previousEvent = events.lastOrNull { it.time.isBefore(localNow) }
        val progress = if (previousEvent == null) {
            0
        } else {
            progressBetween(previousEvent.time, nextEvent.time, localNow)
        }

        val isTomorrow = nextEvent.time.toLocalDate().isAfter(localNow.toLocalDate())
        val status = if (isTomorrow) "Tomorrow" else "Today"

        return SolarEvent(
            label = nextEvent.label,
            displayTime = nextEvent.time.format(formatter),
            timeRemaining = formatRemaining(Duration.between(localNow, nextEvent.time)),
            progressPercent = progress,
            iconText = nextEvent.iconText,
            statusText = status
        )
    }

    private fun progressBetween(start: ZonedDateTime, end: ZonedDateTime, now: ZonedDateTime): Int {
        val total = Duration.between(start, end).toMillis()
        val elapsed = Duration.between(start, now).toMillis()
        if (total <= 0L) return 0

        return ((elapsed.toDouble() / total.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun formatRemaining(duration: Duration): String {
        val safeDuration = if (duration.isNegative) Duration.ZERO else duration
        val hours = safeDuration.toHours()
        val minutes = safeDuration.minusHours(hours).toMinutes()

        return when {
            hours > 0 -> "${hours}h ${minutes}m left"
            minutes > 0 -> "${minutes}m left"
            else -> "Now"
        }
    }

    private data class TimedSolarEvent(
        val label: String,
        val time: ZonedDateTime,
        val iconText: String
    )
}
