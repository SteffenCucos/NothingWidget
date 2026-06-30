package com.steffencucos.nothingwidget.solar

import java.time.ZonedDateTime

enum class SolarBody {
    SUN,
    MOON
}

data class SolarEvent(
    val label: String,
    val displayTime: String,
    val timeRemaining: String,
    val progressPercent: Int,
    val iconText: String,
    val statusText: String,
    val body: SolarBody = SolarBody.SUN,
    val cycleProgress: Float = 0f,
    val cycleStart: ZonedDateTime? = null,
    val cycleEnd: ZonedDateTime? = null,
    val nextEventTime: ZonedDateTime? = null
)
