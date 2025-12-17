package com.example.stepcounter.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stepcounter.R
import com.example.stepcounter.data.AlarmRepository
import com.example.stepcounter.services.AlarmSoundService
import com.example.stepcounter.ui.alarming.AlarmActivity
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

        startAlarmServiceAndActivity(context, alarmId)

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

    private fun startAlarmServiceAndActivity(context: Context, alarmId: Int) {

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            "alarm_channel",
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification =
            NotificationCompat.Builder(context, "alarm_channel")
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("Alarm Ringing")
                .setContentText("Time to wake up!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .build()

        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("NOTIFICATION", notification)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

}