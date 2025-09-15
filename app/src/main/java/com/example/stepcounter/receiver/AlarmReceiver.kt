package com.example.stepcounter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {

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
    }
}