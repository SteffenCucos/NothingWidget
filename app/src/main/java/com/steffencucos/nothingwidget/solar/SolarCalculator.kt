package com.steffencucos.nothingwidget.solar

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object SolarCalculator {
    private const val ZENITH = 90.833

    fun sunrise(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ): ZonedDateTime? = calculate(date, latitude, longitude, zoneId, SolarEventType.SUNRISE)

    fun sunset(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ): ZonedDateTime? = calculate(date, latitude, longitude, zoneId, SolarEventType.SUNSET)

    private fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        type: SolarEventType
    ): ZonedDateTime? {
        val dayOfYear = date.dayOfYear.toDouble()
        val longitudeHour = longitude / 15.0
        val approximateTime = when (type) {
            SolarEventType.SUNRISE -> dayOfYear + ((6.0 - longitudeHour) / 24.0)
            SolarEventType.SUNSET -> dayOfYear + ((18.0 - longitudeHour) / 24.0)
        }

        val meanAnomaly = (0.9856 * approximateTime) - 3.289
        val trueLongitude = normalizeDegrees(
            meanAnomaly +
                (1.916 * sinDeg(meanAnomaly)) +
                (0.020 * sinDeg(2.0 * meanAnomaly)) +
                282.634
        )

        var rightAscension = atan(0.91764 * tanDeg(trueLongitude)).toDegrees()
        rightAscension = normalizeDegrees(rightAscension)

        val longitudeQuadrant = floor(trueLongitude / 90.0) * 90.0
        val ascensionQuadrant = floor(rightAscension / 90.0) * 90.0
        rightAscension += longitudeQuadrant - ascensionQuadrant
        rightAscension /= 15.0

        val sinDeclination = 0.39782 * sinDeg(trueLongitude)
        val cosDeclination = cos(asin(sinDeclination))
        val cosLocalHourAngle =
            (cosDeg(ZENITH) - (sinDeclination * sinDeg(latitude))) /
                (cosDeclination * cosDeg(latitude))

        if (cosLocalHourAngle > 1.0 || cosLocalHourAngle < -1.0) {
            return null
        }

        var localHourAngle = when (type) {
            SolarEventType.SUNRISE -> 360.0 - acos(cosLocalHourAngle).toDegrees()
            SolarEventType.SUNSET -> acos(cosLocalHourAngle).toDegrees()
        }
        localHourAngle /= 15.0

        val localMeanTime = localHourAngle + rightAscension - (0.06571 * approximateTime) - 6.622
        val utcHour = normalizeHours(localMeanTime - longitudeHour)
        val seconds = (utcHour * 3600.0).toLong().coerceIn(0L, 86_399L)

        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.of("UTC"))
            .plusSeconds(seconds)
            .withZoneSameInstant(zoneId)
    }

    private fun normalizeDegrees(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

    private fun normalizeHours(value: Double): Double = ((value % 24.0) + 24.0) % 24.0

    private fun sinDeg(value: Double): Double = sin(value.toRadians())

    private fun cosDeg(value: Double): Double = cos(value.toRadians())

    private fun tanDeg(value: Double): Double = tan(value.toRadians())

    private fun Double.toRadians(): Double = this * PI / 180.0

    private fun Double.toDegrees(): Double = this * 180.0 / PI
}

enum class SolarEventType {
    SUNRISE,
    SUNSET
}
