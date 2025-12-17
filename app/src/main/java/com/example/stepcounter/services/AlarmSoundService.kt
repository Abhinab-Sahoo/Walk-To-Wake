package com.example.stepcounter.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification: Notification? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra("NOTIFICATION", Notification::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra("NOTIFICATION")
            }

        val alarmId = intent?.getIntExtra("ALARM_ID", -1)

// If we have a bad ID or no notification, we can't run. Stop immediately.
        if (alarmId == null || alarmId == -1 || notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }

// If we passed the check, we can safely start.
        startForeground(alarmId, notification)

        startAlarmSound()
        return START_STICKY
    }

    private fun startAlarmSound() {
        if (mediaPlayer == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmSoundService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setOnPreparedListener { player ->
                    player.start()
                }
                prepareAsync()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}