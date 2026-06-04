package com.example.stepcounter.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import com.example.stepcounter.data.repository.AlarmRepository
import com.example.stepcounter.util.AlarmLauncher
import com.example.stepcounter.util.StepCounterState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class AlarmSoundService : Service(), SensorEventListener {

    private var mediaPlayer: MediaPlayer? = null

    @Inject
    lateinit var alarmRepository: AlarmRepository
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var currentAlarmId: Int = -1

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP_SOUND) {
            stopAndReleaseMediaPlayer()
            unRegisterSensorManager()
            stopSelf()
            return START_NOT_STICKY
        }

        // Case 1: Service is restarting after force stop (START_STICKY restarts)
        // Android delivers null intent in this case
        if (intent == null) {
            handleServiceRestart()
            return START_STICKY
        }

        // Case 2: Normal start - extract data from intent
        val alarmId = extractAlarmId(intent)
        val notification = extractNotification(intent)

        // Case 3: Bad data - can't run, stop cleanly
        if (alarmId == -1 || notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Case 4: Everything valid - save ID and start normally
        currentAlarmId = alarmId
        startForeground(alarmId, notification)
        startAlarmSound()
        initializeSensorAndSteps(alarmId)

        return START_STICKY

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            StepCounterState.updateSteps(totalStepsSinceBoot)
        }
        if (StepCounterState.isTargetReached) {
            sensorManager.unregisterListener(this)
        }
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

    // When restarted by Android after force stop
    // check if user still haven't walked enough - if so, retrigger.
    private fun handleServiceRestart() {
        if (currentAlarmId != -1 && !StepCounterState.isTargetReached) {
            AlarmLauncher(this).launchAlarm(currentAlarmId)
        }
    }

    // Extract alarmId form intent safely
    // Returns -1 if missing or invalid.
    private fun extractAlarmId(intent: Intent): Int {
        return intent.getIntExtra("ALARM_ID", -1)
    }

    // Pulls the pre-built notification form intent.
    // Handles API version difference for getParcelableExtra.
    private fun extractNotification(intent: Intent): Notification? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("NOTIFICATION", Notification::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("NOTIFICATION")
        }
    }

    // Fetches alarm from DB, resets step state,
    // then registers the step sensor on the main thread.
    private fun initializeSensorAndSteps(alarmId: Int) {
        serviceScope.launch {
            val alarm = alarmRepository.getAlarmById(alarmId)
            if (alarm != null) {
                StepCounterState.reset(alarm.steps)
            }

            withContext(Dispatchers.Main) {
                sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                if (stepSensor != null) {
                    sensorManager.registerListener(
                        this@AlarmSoundService,
                        stepSensor,
                        SensorManager.SENSOR_DELAY_UI
                    )
                }
            }
        }
    }

    private fun unRegisterSensorManager() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    private fun stopAndReleaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        const val ACTION_STOP_SOUND = "com.example.stepcounter.ACTION_STOP_SOUND"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAndReleaseMediaPlayer()
        unRegisterSensorManager()
        serviceScope.cancel()
    }

}