package com.example.stepcounter.util

import android.annotation.SuppressLint
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

object AlarmTimeHelper {

    fun getTimeUntilAlarm(
        hour: Int,
        minute: Int,
        daysOfWeek: Set<DayOfWeek>
    ) : String {

        val now = LocalDateTime.now()
        val alarmTime = findNextAlarmDataTime(hour, minute, daysOfWeek)
        val duration = Duration.between(now, alarmTime)

        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        return buildAlarmMessage(days, hours, minutes, alarmTime)
    }

    private fun findNextAlarmDataTime(
        hour: Int,
        minute: Int,
        daysOfWeek: Set<DayOfWeek>
    ) : LocalDateTime {

        val now = LocalDateTime.now()
        var candidate = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (daysOfWeek.isEmpty()) {
            if (candidate.isBefore(now) || candidate.isEqual(now)) {
                candidate = candidate.plusDays(1)
            }
            return candidate
        }

        for (i in 0..7) {
            if (daysOfWeek.contains(candidate.dayOfWeek) &&  candidate.isAfter(now)) {
                return candidate
            }
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    @SuppressLint("DefaultLocale")
    private fun buildAlarmMessage(
        days: Long,
        hours: Long,
        minutes: Long,
        alarmTime: LocalDateTime
    ) : String {

        val timeString = String.format(
            "%02d:%02d %s",
            if (alarmTime.hour % 12 == 0) 12 else alarmTime.hour % 12,
            alarmTime.minute,
            if (alarmTime.hour < 12) "AM" else "PM"
        )

        return when {
            days == 0L && hours == 0L && minutes == 0L -> {
                "Alarm set for less than 1 minute from now"
            }
            days == 0L && hours > 0L && minutes > 0L -> {
                "Alarm set for ${hours}h ${minutes}m from now"
            }
            days == 0L && hours > 0L && minutes == 0L -> {
                "Alarm set for ${hours}h from now"
            }
            days == 0L && hours == 0L -> {
                "Alarm set for ${minutes}m from now"
            }
            days == 1L -> {
                "Alarm set for tomorrow at $timeString"
            }
            days < 7L -> {
                val dayName = alarmTime.dayOfWeek.name
                    .lowercase()
                    .replaceFirstChar { it.uppercaseChar() }
                "Alarm set for $dayName at $timeString"
            }
            else -> {
                "Alarm set for $days days from now"
            }
        }

    }

}