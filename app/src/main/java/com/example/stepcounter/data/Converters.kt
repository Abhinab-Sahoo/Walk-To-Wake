package com.example.stepcounter.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {

    @TypeConverter
    fun fromDayOfWeekSet(dayOfWeek: Set<DayOfWeek>) : String {
        return dayOfWeek.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String) : Set<DayOfWeek> {
        if (data.isEmpty()) {
            return emptySet()
        }
        return data.split(",").map { DayOfWeek.valueOf(it) }.toSet()
    }
}