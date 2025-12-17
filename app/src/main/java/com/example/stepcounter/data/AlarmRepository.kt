package com.example.stepcounter.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import com.example.stepcounter.MainActivity
import com.example.stepcounter.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import javax.inject.Inject

class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    @ApplicationContext private val context: Context
    ) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun insertAlarm(alarm: Alarm): Long {
        val id = alarmDao.insert(alarm)
        scheduleAlarm(alarm.copy(id = id.toInt()))
        return id
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.update(alarm)
        if (alarm.isEnabled) {
            scheduleAlarm(alarm)
        } else {
            cancelAlarm(alarm)
        }
    }

    fun scheduleAlarm(alarm: Alarm) {
        val triggerTime = findNextAlarmTime(alarm.hour, alarm.minute, alarm.daysOfWeek)

        val showAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val showAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            showAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showAppPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

    }

    private fun cancelAlarm(alarm: Alarm) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun findNextAlarmTime(hour: Int, minute: Int, daysOfWeek: Set<DayOfWeek>): Long {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (daysOfWeek.isNotEmpty()) {
            for (i in 0..7) {
                val currentDayCode = calendar.get(Calendar.DAY_OF_WEEK)

                val isDaySelected = daysOfWeek.any { day ->
                    val calendarDay = (day.value % 7) +1
                    calendarDay == currentDayCode
                }

                if (isDaySelected && calendar.timeInMillis > System.currentTimeMillis()) {
                    return calendar.timeInMillis
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return calendar.timeInMillis

    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.delete(alarm)
    }

    fun getAllAlarms(): Flow<List<Alarm>> {
        return alarmDao.getAllAlarms()
    }

    suspend fun getAllAlarmsList(): List<Alarm> {
        return alarmDao.getAllAlarmsList()
    }

    suspend fun getAlarmById(creationTime: Int): Alarm? {
        return alarmDao.getAlarmById(creationTime)
    }
}