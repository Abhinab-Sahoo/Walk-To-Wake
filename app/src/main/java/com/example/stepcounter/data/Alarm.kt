package com.example.stepcounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean,
    val label: String,
    val steps: Int = 0
) {
    val formattedTime: String
        get() {
            val time = LocalTime.of(hour, minute)
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            return time.format(formatter)
        }

    val isScheduledForToday: Boolean
        get() {
            val today = LocalDate.now().dayOfWeek
            return daysOfWeek.isEmpty() || daysOfWeek.contains(today)
        }
}
