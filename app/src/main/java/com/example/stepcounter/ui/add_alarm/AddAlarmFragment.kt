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
import androidx.navigation.fragment.navArgs
import com.example.stepcounter.data.Alarm
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

    private val args: AddAlarmFragmentArgs by navArgs()

    // Handles the result from the special "schedule exact alarms" permission screen.
    private val exactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && alarmManager.canScheduleExactAlarms()
            ) {
                scheduleAlarm()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied. Cannot set exact alarm.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Handles the result of the step counter (Activity Recognition) permission request.
    private val stepCounterPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (!granted) {
                Toast.makeText(
                    requireContext(),
                    "Step permission denied. Steps may not work.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // After handling this, proceed to the next permission check.
            ensureNotificationPermission()
        }

    // Handles the result of the notification permission request (Android 13+).
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (!granted) {
                Toast.makeText(
                    requireContext(),
                    "Notification permission denied. Alarm may not show.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // After handling this, proceed to the final permission check.
            ensureExactAlarmPermission()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddAlarmBinding.inflate(inflater, container, false)
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        alarmViewModel.initialize(args.alarm)
        observeAlarmData()
        binding.saveAlarmButton.setOnClickListener {
            onSaveClicked()
        }

        observeUiEvents()

    }

    private fun observeAlarmData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmViewModel.alarmToEdit.collect { alarm ->
                    alarm?.let {
                        fillUi(it)
                    }
                }
            }
        }
    }

    private fun fillUi(alarm: Alarm) {
        binding.timePicker.hour = alarm.hour
        binding.timePicker.minute = alarm.minute
        binding.labelEditText.setText(alarm.label)
        binding.stepsEditText.setText(alarm.steps.toString())
        setChipsFromDays(alarm.daysOfWeek)

        binding.saveAlarmButton.text = "Update"
    }

    private fun setChipsFromDays(days: Set<DayOfWeek>) {
        val dayToChipMap = mapOf(
            DayOfWeek.MONDAY to binding.mondayChip,
            DayOfWeek.TUESDAY to binding.tuesdayChip,
            DayOfWeek.WEDNESDAY to binding.wednesdayChip,
            DayOfWeek.THURSDAY to binding.thursdayChip,
            DayOfWeek.FRIDAY to binding.fridayChip,
            DayOfWeek.SATURDAY to binding.saturdayChip,
            DayOfWeek.SUNDAY to binding.sundayChip
        )

        days.forEach { day ->
            dayToChipMap[day]?.isChecked = true
        }
    }

    /**
     * Subscribes to one-time events from the ViewModel, like showing a toast.
     */
    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmViewModel.alarmScheduledEvent.collect { event ->
                    if (event is AddAlarmUiEvent.ShowToast) {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    /**
     * Entry point when the user clicks the save button. Starts the chain of permission checks.
     */
    private fun onSaveClicked() {
        val steps = binding.stepsEditText.text.toString().toIntOrNull() ?: 0
        if (steps > 0) {
            // If steps are required, start with the step counter permission.
            ensureStepCounterPermission()
        } else {
            // Otherwise, skip to the notification permission.
            ensureNotificationPermission()
        }
    }

    /**
     * Checks for and requests the Step Counter (Activity Recognition) permission if needed.
     */
    private fun ensureStepCounterPermission() {
        // This permission is only required on Android Q (10) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ensureNotificationPermission()
            return
        }

        val perm = Manifest.permission.ACTIVITY_RECOGNITION
        when {
            ContextCompat.checkSelfPermission(requireContext(), perm)
                    == PackageManager.PERMISSION_GRANTED -> {
                ensureNotificationPermission()
            }

            // TODO: Show a custom dialog, explaining why we need this permission.
            shouldShowRequestPermissionRationale(perm) -> {
                stepCounterPermissionLauncher.launch(perm)
            }

            else -> {
                // Request the permission.
                stepCounterPermissionLauncher.launch(perm)
            }
        }
    }

    /**
     * Checks for and requests the Notification permission if needed.
     */
    private fun ensureNotificationPermission() {
        // This permission is only required on Android Tiramisu (13) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ensureExactAlarmPermission()
            return
        }

        val perm = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(requireContext(), perm)
            == PackageManager.PERMISSION_GRANTED
        ) {
            ensureExactAlarmPermission()
        } else {
            // Request the permission.
            notificationPermissionLauncher.launch(perm)
        }
    }

    /**
     * Checks if the app can schedule exact alarms and requests permission if needed.
     */
    private fun ensureExactAlarmPermission() {
        // This permission is only required on Android S (12) and above.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            scheduleAlarm()
            return
        }

        if (alarmManager.canScheduleExactAlarms()) {
            scheduleAlarm()
        } else {
            // This is a special permission that takes the user to a system screen.
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            exactAlarmPermissionLauncher.launch(intent)
        }
    }

    /**
     * Gathers all user input from the UI and tells the ViewModel to schedule the alarm.
     * This is the final step after all permissions are granted.
     */
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

    /**
     * Helper function to determine which days of the week have been selected via the chips.
     * @return A Set of DayOfWeek enums representing the checked days.
     */
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

        // Filters the map for checked chips and returns the corresponding DayOfWeek values.
        return chipToDayMap.filter { (chip, _) -> chip.isChecked }.values.toSet()
    }

    /**
     * Cleans up the binding reference when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
