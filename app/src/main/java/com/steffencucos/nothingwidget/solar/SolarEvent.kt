package com.steffencucos.nothingwidget.solar

data class SolarEvent(
    val label: String,
    val displayTime: String,
    val timeRemaining: String,
    val progressPercent: Int,
    val iconText: String,
    val statusText: String
)
