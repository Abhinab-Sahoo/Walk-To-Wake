package com.example.stepcounter.ui.alarming

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.stepcounter.R
import com.example.stepcounter.databinding.ActivityAlarmBinding
import com.example.stepcounter.services.AlarmSoundService
import com.example.stepcounter.util.SnoozeHelper
import com.example.stepcounter.util.StepCounterState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private val alarmingViewModel: AlarmingViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpWindowFlags()

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeAlarmData()
        onSnoozeClicked()
        onDismissClicked()
        observeStepProgress()

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

    private fun onSnoozeClicked() {
        binding.snoozeButton.setOnClickListener {
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            if (alarmId == -1) return@setOnClickListener

            val snoozeIntent = Intent(this, AlarmSoundService::class.java).apply {
                action = AlarmSoundService.ACTION_STOP_SOUND
            }
            startService(snoozeIntent)

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(alarmId)

            SnoozeHelper(this).scheduleSnooze(alarmId, 5)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
        }
    }

    private fun onDismissClicked() {
        binding.dismissButton.setOnClickListener {
            // Stop Sound
            stopService(Intent(this, AlarmSoundService::class.java))
            // Update DB (Disable alarm if it's one-time)
            val alarm = alarmingViewModel.alarm.value
            if (alarm != null && alarm.daysOfWeek.isEmpty()) {
                alarmingViewModel.update(alarm.copy(isEnabled = false))
            }
            // Cancel Notification
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            if (alarmId != -1) {
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(alarmId)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
        }
    }

    private fun observeAlarmData() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmingViewModel.alarm.collect { alarm ->
                    if (alarm != null) {
                        binding.alarmLabelTextView.text = alarm.label

                        if (alarm.steps > 0) {
                            binding.currentStepsTextView.visibility = View.VISIBLE
                            binding.stepsProgressBar.visibility = View.VISIBLE
                            binding.targetStepsTextView.visibility = View.VISIBLE

                            binding.stepsProgressBar.max = alarm.steps
                            binding.targetStepsTextView.text =
                                getString(R.string.steps_to_walk, alarm.steps)
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
    }

    private fun observeStepProgress() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                StepCounterState.currentSteps.collect { steps ->
                    val requiredSteps = StepCounterState.requiredSteps.value
                    if (requiredSteps == 0) return@collect

                    val displaySteps = minOf(steps, requiredSteps)
                    binding.currentStepsTextView.text = displaySteps.toString()
                    binding.stepsProgressBar.progress = displaySteps

                    if (StepCounterState.isTargetReached) {
                        binding.dismissButton.isEnabled = true
                        binding.dismissButton.alpha = 1f
                    }
                }
            }
        }
    }
}