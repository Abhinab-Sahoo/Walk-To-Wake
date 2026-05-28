package com.example.stepcounter.ui.add_alarm

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.stepcounter.R
import com.example.stepcounter.data.local.Alarm
import com.example.stepcounter.databinding.FragmentAddAlarmBinding
import com.example.stepcounter.util.DaySelector
import com.example.stepcounter.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.DayOfWeek


@AndroidEntryPoint
class AddAlarmFragment : Fragment() {

    // TODO: Handle scenario if user denies notification permission.

    private var _binding: FragmentAddAlarmBinding? = null
    private val binding get() = _binding!!
    private lateinit var alarmManager: AlarmManager
    private val addAlarmViewModel: AddAlarmViewModel by viewModels()

    private val args: AddAlarmFragmentArgs by navArgs()

    private lateinit var daySelector: DaySelector
    private lateinit var permissionHelper: PermissionHelper

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
            if (granted) {
                permissionHelper.ensureNotificationPermission()
            } else {
                permissionHelper.showStepCounterPermissionRequiredDialog()
            }
        }

    // Handles the result of the notification permission request (Android 13+).
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted) {
                permissionHelper.ensureExactAlarmPermission()
            } else {
                permissionHelper.showNotificationPermissionRequiredDialog()
            }
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

        addAlarmViewModel.initialize(args.alarm)

        setupDaySelector()
        observeAlarmData()

        binding.saveAlarmButton.setOnClickListener {
            onSaveClicked()
        }

        initializePermissionHelper()
        observeUiEvents()

    }

    private fun observeAlarmData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                addAlarmViewModel.alarmToEdit.collect { alarm ->
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
        binding.stepsEditText.setText(
            if (alarm.steps >= 0) alarm.steps.toString() else ""
        )
        daySelector.setSelectedDays(alarm.daysOfWeek)

        binding.saveAlarmButton.text = getString(R.string.update)
    }

    private fun setupDaySelector() {
        daySelector = DaySelector(
            mapOf(
                DayOfWeek.SUNDAY to binding.btnSun,
                DayOfWeek.MONDAY to binding.btnMon,
                DayOfWeek.TUESDAY to binding.btnTue,
                DayOfWeek.WEDNESDAY to binding.btnWed,
                DayOfWeek.THURSDAY to binding.btnThu,
                DayOfWeek.FRIDAY to binding.btnFri,
                DayOfWeek.SATURDAY to binding.btnSat
            )
        )
    }

    /**
     * Subscribes to one-time events from the ViewModel, like showing a toast.
     */
    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                addAlarmViewModel.alarmScheduledEvent.collect { event ->
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
        permissionHelper.startPermissionChain(steps > 0)
    }

    fun initializePermissionHelper() {

        permissionHelper = PermissionHelper(
            fragment = this,
            onAllPermissionReady = { scheduleAlarm() },
            onStepPermissionNeeded = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    stepCounterPermissionLauncher.launch(
                        Manifest.permission.ACTIVITY_RECOGNITION
                    )
                }
            },
            onNotificationPermissionNeeded = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            },
            onExactAlarmPermissionNeeded = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                } else {
                    TODO("VERSION.SDK_INT < S")
                }
                exactAlarmPermissionLauncher.launch(intent)
            }
        )
    }

    /**
     * Gathers all user input from the UI and tells the ViewModel to schedule the alarm.
     * This is the final step after all permissions are granted.
     */
    private fun scheduleAlarm() {
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val label = binding.labelEditText.text.toString()
        val steps = binding.stepsEditText.text.toString().toIntOrNull() ?: 0
        val selectedDays = daySelector.getSelectedDays()

        addAlarmViewModel.scheduleAlarm(
            hour = hour,
            minute = minute,
            daysOfWeek = selectedDays,
            label = label,
            steps = steps
        )
    }

    /**
     * Cleans up the binding reference when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
