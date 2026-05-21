package com.example.stepcounter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.stepcounter.data.repository.AlarmRepository
import com.example.stepcounter.util.AlarmLauncher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmRepository

    override fun onReceive(context: Context?, intent: Intent?) {

        if (context == null) return

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        if (alarmId == -1) return

        AlarmLauncher(context).launchAlarm(alarmId)

        /*
        goAsync() is a method of the BroadcastReceiver class used to perform asynchronous
        work within the onReceive() method. By default a BroadcastReceiver is considered
        finished as soon as onReceive() returns, allowing the system to potentially kill
        the process. Calling goAsync() signals the system that the receiver is still active
        even after returning from the main function, giving you a small window (typically 10sec)
        to complete background task.
        You must call finish() on the PendingResult object once your background work is done
        to signal the system that it can reclaim the process now.

        If you suspect your database or repository operation could exceed 10 seconds,
        do not use goAsync(). Hand it over to WorkManager.
         */

        val pendingIntent = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = repository.getAlarmById(alarmId)

                if (alarm != null && alarm.daysOfWeek.isNotEmpty()) {
                    repository.scheduleAlarm(alarm)
                    Log.d("AlarmReceiver", "Rescheduled alarm ${alarm.id} for next occurrence")
                } else {
                    if (alarm != null) {
                        repository.updateAlarm(alarm.copy(isEnabled = false))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingIntent.finish()
            }
        }

    }

}