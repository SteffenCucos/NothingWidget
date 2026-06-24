package com.steffencucos.nothingwidget.widget

enum class WidgetAccentColor(
    val displayName: String,
    val argb: Int
) {
    RED("Red", 0xFFC43A36.toInt()),
    ORANGE("Orange", 0xFFFF7A1A.toInt()),
    YELLOW("Yellow", 0xFFE6B800.toInt()),
    GREEN("Green", 0xFF44B26F.toInt()),
    CYAN("Cyan", 0xFF2EB8C5.toInt()),
    BLUE("Blue", 0xFF4D7CFE.toInt()),
    PURPLE("Purple", 0xFF9B5CFF.toInt()),
    PINK("Pink", 0xFFE0528D.toInt());

    companion object {
        fun fromName(value: String?): WidgetAccentColor = entries
            .firstOrNull { it.name == value }
            ?: RED
    }
}
