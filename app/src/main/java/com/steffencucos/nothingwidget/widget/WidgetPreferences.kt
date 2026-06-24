package com.steffencucos.nothingwidget.widget

import android.content.Context

object WidgetPreferences {
    private const val PREFS_NAME = "nothing_widget_preferences"
    private const val KEY_WIDGET_STYLE = "widget_style"

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
}
