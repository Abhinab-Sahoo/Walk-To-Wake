package com.example.stepcounter.ui.add_alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.stepcounter.databinding.FragmentAddAlarmBinding
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
                    scheduleAlarm()
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
                Toast.makeText(
                    requireContext(),
                    "Step counting permission is denied.",
                    Toast.LENGTH_SHORT
                ).show()
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

        collectUiEvents()

    }

    private fun collectUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmViewModel.alarmScheduledEvent.collect { event ->
                    when (event) {
                        is AddAlarmUiEvent.ShowToast -> {
                            Toast.makeText(
                                requireContext(),
                                event.message,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                    }
                }
            }
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

    private fun scheduleAlarm() {

        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val label = binding.labelEditText.text.toString().ifEmpty { "Alarm" }
        val steps = binding.stepsEditText.text.toString().toIntOrNull() ?: 0
        val selectedDays = getSelectedDays()

        alarmViewModel.schedule(
            hour = hour,
            minute = minute,
            daysOfWeek = selectedDays,
            label = label,
            steps = steps
        )

    }

    private fun getSelectedDays(): Set<DayOfWeek> {
        val chipToDayMap = mapOf(
            binding.mondayChip to DayOfWeek.MONDAY,
            binding.tuesdayChip to DayOfWeek.TUESDAY,
            binding.wednesdayChip to DayOfWeek.WEDNESDAY,
            binding.thursdayChip to DayOfWeek.THURSDAY,
            binding.fridayChip to DayOfWeek.FRIDAY,
            binding.saturdayChip to DayOfWeek.SATURDAY,
            binding.sundayChip to DayOfWeek.SUNDAY
        )

        return chipToDayMap.filter { (chip, _) -> chip.isChecked }.values.toSet()
    }

    private fun saveAlarmWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                scheduleAlarm()
            } else {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                )
                requestExactAlarmPermissionLauncher.launch(intent)
            }
        } else {
            scheduleAlarm()
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