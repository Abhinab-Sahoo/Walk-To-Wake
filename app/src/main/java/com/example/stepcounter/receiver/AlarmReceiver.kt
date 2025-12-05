package com.example.stepcounter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.stepcounter.data.AlarmRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        val time = intent?.getStringExtra("alarm_time")

        Toast.makeText(context, "⏰ Alarm triggered at $time", Toast.LENGTH_LONG).show()

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone.play()

        Handler(Looper.getMainLooper()).postDelayed({
            if (ringtone.isPlaying) {
                ringtone.stop()
            }
        }, 5000)

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        if (alarmId == -1) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = alarmRepository.getAlarmById(alarmId)

            if (alarm != null) {
                if (alarm.daysOfWeek.isEmpty()) {
                    val updatedAlarm = alarm.copy(isEnabled = false)
                    alarmRepository.updateAlarm(updatedAlarm)
                }
            }
        }
    }
}