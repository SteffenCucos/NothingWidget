package com.steffencucos.nothingwidget.widget

object DotMatrixText {
    private const val DOT = '\u2022'

    private val glyphs = mapOf(
        'A' to arrayOf("010", "101", "111", "101", "101"),
        'E' to arrayOf("111", "100", "110", "100", "111"),
        'F' to arrayOf("111", "100", "110", "100", "100"),
        'I' to arrayOf("111", "010", "010", "010", "111"),
        'L' to arrayOf("100", "100", "100", "100", "111"),
        'M' to arrayOf("101", "111", "111", "101", "101"),
        'N' to arrayOf("101", "111", "111", "111", "101"),
        'O' to arrayOf("010", "101", "101", "101", "010"),
        'P' to arrayOf("110", "101", "110", "100", "100"),
        'R' to arrayOf("110", "101", "110", "101", "101"),
        'S' to arrayOf("011", "100", "010", "001", "110"),
        'T' to arrayOf("111", "010", "010", "010", "010"),
        'U' to arrayOf("101", "101", "101", "101", "111"),
        'W' to arrayOf("101", "101", "111", "111", "101"),
        'X' to arrayOf("101", "101", "010", "101", "101"),
        'Y' to arrayOf("101", "101", "010", "010", "010"),
        '0' to arrayOf("111", "101", "101", "101", "111"),
        '1' to arrayOf("010", "110", "010", "010", "111"),
        '2' to arrayOf("110", "001", "010", "100", "111"),
        '3' to arrayOf("110", "001", "010", "001", "110"),
        '4' to arrayOf("101", "101", "111", "001", "001"),
        '5' to arrayOf("111", "100", "110", "001", "110"),
        '6' to arrayOf("011", "100", "111", "101", "111"),
        '7' to arrayOf("111", "001", "010", "010", "010"),
        '8' to arrayOf("111", "101", "111", "101", "111"),
        '9' to arrayOf("111", "101", "111", "001", "110"),
        ':' to arrayOf("0", "1", "0", "1", "0"),
        ' ' to arrayOf("0", "0", "0", "0", "0")
    )

    fun render(value: String, maxCharacters: Int = 8): String {
        val normalized = value.uppercase().filter { glyphs.containsKey(it) }.take(maxCharacters)
        if (normalized.isBlank()) return value.uppercase()

        val rows = MutableList(5) { StringBuilder() }
        normalized.forEachIndexed { charIndex, char ->
            val glyph = glyphs.getValue(char)
            glyph.forEachIndexed { rowIndex, pattern ->
                pattern.forEach { pixel -> rows[rowIndex].append(if (pixel == '1') DOT else ' ') }
                if (charIndex != normalized.lastIndex) rows[rowIndex].append(' ')
            }
        }
        return rows.joinToString("\n") { it.toString().trimEnd() }
    }
}
