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
        val localNow = now.withZoneSameInstant(zoneId)
        val today = localNow.toLocalDate()
        val events = (-2L..3L)
            .flatMap { offset ->
                val date = today.plusDays(offset)
                listOfNotNull(
                    SolarCalculator.sunrise(date, location.latitude, location.longitude, zoneId)
                        ?.let { TimedSolarEvent(SUNRISE_LABEL, it) },
                    SolarCalculator.sunset(date, location.latitude, location.longitude, zoneId)
                        ?.let { TimedSolarEvent(SUNSET_LABEL, it) }
                )
            }
            .distinctBy { it.label to it.time.toInstant() }
            .sortedBy { it.time }

        if (events.isEmpty()) {
            return SolarEvent(
                label = "Solar event",
                displayTime = "--:--",
                timeRemaining = "Unavailable here",
                progressPercent = 0,
                iconText = "◎",
                statusText = "No sunrise/sunset"
            )
        }

        val nextEvent = events.firstOrNull { it.time.isAfter(localNow) } ?: events.first()
        val previousEvent = events
            .lastOrNull { !it.time.isAfter(localNow) }
            ?: events.lastOrNull { it.time.isBefore(nextEvent.time) }

        val cycleStart = previousEvent?.time
        val cycleEnd = nextEvent.time
        val cycleProgress = if (cycleStart == null) {
            0f
        } else {
            progressBetween(cycleStart, cycleEnd, localNow)
        }
        val progressPercent = (cycleProgress * 100f).roundToInt().coerceIn(0, 100)
        val body = bodyForInterval(nextEvent.label)
        val isTomorrow = nextEvent.time.toLocalDate().isAfter(localNow.toLocalDate())
        val status = if (isTomorrow) "Tomorrow" else "Today"

        return SolarEvent(
            label = nextEvent.label,
            displayTime = nextEvent.time.format(formatter),
            timeRemaining = formatRemaining(Duration.between(localNow, nextEvent.time)),
            progressPercent = progressPercent,
            iconText = iconFor(body),
            statusText = status,
            body = body,
            cycleProgress = cycleProgress,
            cycleStart = cycleStart,
            cycleEnd = cycleEnd,
            nextEventTime = nextEvent.time
        )
    }

    private fun bodyForInterval(nextEventLabel: String): SolarBody =
        if (nextEventLabel == SUNSET_LABEL) SolarBody.SUN else SolarBody.MOON

    private fun iconFor(body: SolarBody): String = when (body) {
        SolarBody.SUN -> "☀"
        SolarBody.MOON -> "☾"
    }

    private fun progressBetween(start: ZonedDateTime, end: ZonedDateTime, now: ZonedDateTime): Float {
        val total = Duration.between(start, end).toMillis()
        val elapsed = Duration.between(start, now).toMillis()
        if (total <= 0L) return 0f

        return (elapsed.toDouble() / total.toDouble())
            .toFloat()
            .coerceIn(0f, 1f)
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
        val time: ZonedDateTime
    )

    companion object {
        private const val SUNRISE_LABEL = "Sunrise"
        private const val SUNSET_LABEL = "Sunset"
    }
}
