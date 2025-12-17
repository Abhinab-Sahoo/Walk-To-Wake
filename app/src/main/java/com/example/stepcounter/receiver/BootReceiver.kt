package com.example.stepcounter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.stepcounter.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: AlarmRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {

            val pendingIntent = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = repository.getAllAlarmsList()

                    alarms.forEach { alarm ->
                        if (alarm.isEnabled) {
                            repository.scheduleAlarm(alarm)
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
}