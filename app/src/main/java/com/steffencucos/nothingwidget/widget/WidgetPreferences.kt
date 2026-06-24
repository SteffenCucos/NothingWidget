package com.steffencucos.nothingwidget.widget

import android.content.Context

object WidgetPreferences {
    const val MIN_DOT_TEXT_SIZE_SP = 5
    const val MAX_DOT_TEXT_SIZE_SP = 10
    const val DEFAULT_DOT_TEXT_SIZE_SP = 7

    private const val PREFS_NAME = "nothing_widget_preferences"
    private const val KEY_WIDGET_STYLE = "widget_style"
    private const val KEY_DOT_TEXT_SIZE_SP = "dot_text_size_sp"

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
}
