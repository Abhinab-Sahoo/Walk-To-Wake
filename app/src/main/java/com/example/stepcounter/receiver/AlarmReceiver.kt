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