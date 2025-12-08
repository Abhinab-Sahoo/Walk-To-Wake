package com.example.stepcounter.ui.add_alarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.databinding.FragmentAddAlarmBinding
import com.example.stepcounter.receiver.AlarmReceiver
import com.example.stepcounter.ui.alarm.AlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.DayOfWeek


@AndroidEntryPoint
class AddAlarmFragment : Fragment() {

    private var _binding: FragmentAddAlarmBinding? = null
    private val binding get() = _binding!!
    private lateinit var alarmManager: AlarmManager
    private val alarmViewModel: AlarmViewModel by viewModels()

    private val requestExactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    lifecycleScope.launch { scheduleAlarm() }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Permission denied. Cannot set exact alarm.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val requestStepCounterPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                saveAlarmWithPermissionCheck()
            } else {
                Toast.makeText(requireContext(), "Step counting permission is denied.", Toast.LENGTH_SHORT).show()
                saveAlarmWithPermissionCheck()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAddAlarmBinding.inflate(inflater, container, false)
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveAlarmButton.setOnClickListener {
            onSaveClicked()
        }

    }

    private fun onSaveClicked() {
        val steps = binding.stepsEditText.text.toString().toIntOrNull() ?: 0
        if (steps > 0) {
            checkAndRequestStepCounterPermission()
        } else {
            saveAlarmWithPermissionCheck()
        }
    }

    private suspend fun scheduleAlarm() {

        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val label = binding.labelEditText.text.toString().ifEmpty { "Alarm" }
        val steps = binding.stepsEditText.text.toString().toIntOrNull() ?: 0
        val selectedDays = mutableSetOf<DayOfWeek>()
        if (binding.mondayChip.isChecked) selectedDays.add(DayOfWeek.MONDAY)
        if (binding.tuesdayChip.isChecked) selectedDays.add(DayOfWeek.TUESDAY)
        if (binding.wednesdayChip.isChecked) selectedDays.add(DayOfWeek.WEDNESDAY)
        if (binding.thursdayChip.isChecked) selectedDays.add(DayOfWeek.THURSDAY)
        if (binding.fridayChip.isChecked) selectedDays.add(DayOfWeek.FRIDAY)
        if (binding.saturdayChip.isChecked) selectedDays.add(DayOfWeek.SATURDAY)
        if (binding.sundayChip.isChecked) selectedDays.add(DayOfWeek.SUNDAY)


        val newAlarm = Alarm(
            hour = hour,
            minute = minute,
            daysOfWeek = selectedDays,
            isEnabled = true,
            label = label,
            steps = steps
        )

        val alarmId = alarmViewModel.insert(newAlarm).toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (selectedDays.isEmpty() &&
            calendar.timeInMillis <= System.currentTimeMillis()
        ) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alarmIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            alarmId,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(requireContext(), "Alarm set!", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()

    } // End of scheduleAlarm()

    private fun saveAlarmWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                lifecycleScope.launch { scheduleAlarm() }
            } else {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                )
                requestExactAlarmPermissionLauncher.launch(intent)
            }
        } else {
            lifecycleScope.launch { scheduleAlarm() }
        }
    }

    private fun checkAndRequestStepCounterPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    saveAlarmWithPermissionCheck()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION) -> {
                    // For now, we'll request it directly.
                    // Professionally ask second time if allowed then request
                    requestStepCounterPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                else -> {
                    requestStepCounterPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            saveAlarmWithPermissionCheck()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}