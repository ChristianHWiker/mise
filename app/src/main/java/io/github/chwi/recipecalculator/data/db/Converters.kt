package io.github.chwi.recipecalculator.data.db

import androidx.room.TypeConverter

/** Room type converters. */
class Converters {

    // Steps are a short ordered list; stored joined by the ASCII Unit Separator (0x1F),
    // which never appears in human-entered recipe text.
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        val SEPARATOR = Char(0x1F).toString()
    }
}
