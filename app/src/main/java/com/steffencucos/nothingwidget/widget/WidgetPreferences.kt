package com.steffencucos.nothingwidget.widget

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object WidgetPreferences {
    const val MIN_DOT_TEXT_SIZE_SP = 5
    const val MAX_DOT_TEXT_SIZE_SP = 10
    const val DEFAULT_DOT_TEXT_SIZE_SP = 7
    const val TIME_SIMULATION_MULTIPLIER = 60L

    private const val PREFS_NAME = "nothing_widget_preferences"
    private const val KEY_WIDGET_STYLE = "widget_style"
    private const val KEY_DOT_TEXT_SIZE_SP = "dot_text_size_sp"
    private const val KEY_TIME_SIMULATION_ENABLED = "time_simulation_enabled"
    private const val KEY_TIME_SIMULATION_REAL_ANCHOR_MS = "time_simulation_real_anchor_ms"
    private const val KEY_TIME_SIMULATION_CLOCK_ANCHOR_MS = "time_simulation_clock_anchor_ms"

    fun getStyle(context: Context): WidgetStyle {
        val savedValue = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WIDGET_STYLE, WidgetStyle.CLASSIC.name)

        return runCatching {
            WidgetStyle.valueOf(savedValue ?: WidgetStyle.CLASSIC.name)
        }.getOrDefault(WidgetStyle.CLASSIC)
    }

    fun setStyle(context: Context, style: WidgetStyle) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WIDGET_STYLE, style.name)
            .apply()
    }

    fun getDotTextSizeSp(context: Context): Int = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_DOT_TEXT_SIZE_SP, DEFAULT_DOT_TEXT_SIZE_SP)
        .coerceIn(MIN_DOT_TEXT_SIZE_SP, MAX_DOT_TEXT_SIZE_SP)

    fun setDotTextSizeSp(context: Context, sizeSp: Int) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(
                KEY_DOT_TEXT_SIZE_SP,
                sizeSp.coerceIn(MIN_DOT_TEXT_SIZE_SP, MAX_DOT_TEXT_SIZE_SP)
            )
            .apply()
    }

    fun isTimeSimulationEnabled(context: Context): Boolean = context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_TIME_SIMULATION_ENABLED, false)

    fun setTimeSimulationEnabled(context: Context, enabled: Boolean) {
        val nowMs = System.currentTimeMillis()
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TIME_SIMULATION_ENABLED, enabled)
            .putLong(KEY_TIME_SIMULATION_REAL_ANCHOR_MS, nowMs)
            .putLong(KEY_TIME_SIMULATION_CLOCK_ANCHOR_MS, nowMs)
            .apply()
    }

    fun currentWidgetTime(context: Context): ZonedDateTime {
        if (!isTimeSimulationEnabled(context)) return ZonedDateTime.now()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val realAnchorMs = prefs.getLong(KEY_TIME_SIMULATION_REAL_ANCHOR_MS, System.currentTimeMillis())
        val clockAnchorMs = prefs.getLong(KEY_TIME_SIMULATION_CLOCK_ANCHOR_MS, realAnchorMs)
        val elapsedRealMs = (System.currentTimeMillis() - realAnchorMs).coerceAtLeast(0L)
        val simulatedMs = clockAnchorMs + elapsedRealMs * TIME_SIMULATION_MULTIPLIER

        return Instant.ofEpochMilli(simulatedMs).atZone(ZoneId.systemDefault())
    }
}
