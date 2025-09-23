package com.chris.culsi.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromAction(value: DiscardAction?): String? = value?.name
    @TypeConverter
    fun toAction(value: String?): DiscardAction? = value?.let { DiscardAction.valueOf(it) }
}
