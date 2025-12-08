package com.example.stepcounter.ui.alarming

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.stepcounter.databinding.ActivityAlarmBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private val alarmingViewModel: AlarmingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        if (alarmId == -1) {
            finish()
            return
        }

        lifecycleScope.launch {
            alarmingViewModel.alarm.collect { alarm ->
                if (alarm != null) {
                    binding.digitalClock.text = alarm.formattedHourMinute
                    binding.alarmLabelTextView.text = alarm.label
                    binding.stepsProgressBar.max = alarm.steps
                    binding.targetStepsTextView.text = "/ ${alarm.steps} Steps"
                    binding.currentStepsTextView.text = "0"
                    binding.stepsProgressBar.progress = 0
                }
            }
        }

    }
}