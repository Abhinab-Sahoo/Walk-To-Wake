package com.example.stepcounter.ui.alarming

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stepcounter.R
import com.example.stepcounter.databinding.ActivityAlarmBinding
import com.example.stepcounter.services.AlarmSoundService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AlarmActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityAlarmBinding
    private val alarmingViewModel: AlarmingViewModel by viewModels()

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var initialSteps = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpWindowFlags()

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpSensor()
        observeAlarmData()
        onDismissClicked()

    }

    private fun setUpWindowFlags() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setUpSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    private fun onDismissClicked() {
        binding.dismissButton.setOnClickListener {
            // Stop Sound
            stopService(Intent(this, AlarmSoundService::class.java))
            // Stop Sensor
            sensorManager.unregisterListener(this)
            // Update DB (Disable alarm if it's one-time)
            val alarm = alarmingViewModel.alarm.value
            if (alarm != null && alarm.daysOfWeek.isEmpty()) {
                alarmingViewModel.update(alarm.copy(isEnabled = false))
            }
            // Cancel Notification
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            if (alarmId != -1) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(alarmId)
            }

            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        registerSensorListener()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()

            if (initialSteps == -1) {
                initialSteps = totalStepsSinceBoot
            }
            val alarm = alarmingViewModel.alarm.value ?: return
            val currentSteps = totalStepsSinceBoot - initialSteps

            val stepsToShow = minOf(currentSteps, alarm.steps)
            binding.currentStepsTextView.text = stepsToShow.toString()
            binding.stepsProgressBar.progress = currentSteps


            if (currentSteps >= alarm.steps) {
                binding.dismissButton.isEnabled = true
                binding.dismissButton.alpha = 1f
            }
        }
    }


    private fun registerSensorListener() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            binding.stepsProgressBar.visibility = View.GONE
            binding.currentStepsTextView.visibility = View.GONE
            Toast.makeText(
                this, getString(R.string.sensor_not_available),
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun observeAlarmData() {

        lifecycleScope.launch {
            alarmingViewModel.alarm.collect { alarm ->
                if (alarm != null) {
                    binding.digitalClock.text = alarm.formattedHourMinute
                    binding.alarmLabelTextView.text = alarm.label

                    if (alarm.steps > 0) {
                        binding.currentStepsTextView.visibility = View.VISIBLE
                        binding.stepsProgressBar.visibility = View.VISIBLE
                        binding.targetStepsTextView.visibility = View.VISIBLE

                        binding.stepsProgressBar.max = alarm.steps
                        binding.targetStepsTextView.text = "/ ${alarm.steps} Steps"
                        binding.currentStepsTextView.text = "0"
                        binding.stepsProgressBar.progress = 0
                    } else {
                        binding.dismissButton.isEnabled = true
                        binding.dismissButton.alpha = 1f
                    }

                }
            }
        }
    }
    // TODO: on production alarm apps when user dismisses alarm with device locked, it stays on lock screen.
    // TODO: But on our case after dismissing we were being taken to the alarm screen which is (AlarmFragment).
}