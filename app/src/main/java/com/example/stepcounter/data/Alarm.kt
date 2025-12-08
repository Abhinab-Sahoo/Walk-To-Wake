package com.example.stepcounter.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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

    val formattedHourMinute: String
        get() {
            val time = LocalTime.of(hour, minute)
            val formatter = DateTimeFormatter.ofPattern("hh:mm")
            return time.format(formatter)
        }

    val formattedDays: String
        get() {
            if (daysOfWeek.isEmpty()) {

                if (!isEnabled) {
                    return "Not scheduled"
                }

                val alarmDataTime = LocalTime.of(hour, minute).atDate(LocalDate.now())
                return if (alarmDataTime.isBefore(LocalDateTime.now())) {
                    "Tomorrow"
                } else {
                    "Today"
                }
            }

            if (daysOfWeek.size == 7) {
                return "Every day"
            }

            val sortedDays = daysOfWeek.sortedBy { it.value }

            return sortedDays.joinToString(", ") { day ->
                day.name.lowercase().take(3)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
}
