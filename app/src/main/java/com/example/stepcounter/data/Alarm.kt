package com.example.stepcounter.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val isEnabled: Boolean,
    val label: String
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
