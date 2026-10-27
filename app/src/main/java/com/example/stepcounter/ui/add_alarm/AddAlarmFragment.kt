package com.example.stepcounter.ui.add_alarm

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
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
import com.google.android.material.snackbar.Snackbar
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

    private val ringtoneLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            // result.resultCode tells us if user tapped OK or pressed back
            if (result.resultCode == Activity.RESULT_OK) {

                // RingtoneManager puts the picked Uri into the Intent data
                // getParcelableExtra pulls it out - it's a Uri object
                val uri: Uri? = result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI
                )

                // uri can be null here - it means user picked "Silent"
                // We convert Uri to String for storage (Room saves string not Uri objects)
                // uri?.toString() returns null if uri is null - so null = silent
                addAlarmViewModel.onRingtoneSelected(uri?.toString())

                // Update the text field to show the ringtone name
//                updateRingtoneLabel(uri)
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

        binding.ringtoneEditText.setOnClickListener {
            openRingtonePicker()
        }

        observeSelectedRingtone()

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

    private fun observeSelectedRingtone() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                addAlarmViewModel.selectedRingtoneUri.collect { uriString ->
                    updateRingtoneLabel(uriString)
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
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
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

    private fun openRingtonePicker() {

        // Get whatever URI is currently selected in the ViewModel
        val currentUri: Uri? = addAlarmViewModel.selectedRingtoneUri.value?.toUri() // converts saved string to Uri

        // Build the Intent that opens the system ringtone picker
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {

            // Show only alarm sounds (not ringtone or notifications)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)

            // Show default options at the top of the list
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)

            // Show "Silent" options (user may want no sound)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)

            // What "Default" points to - the device's default alarm sound
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )

            // Pass the currently selected URI so the picker highlights it
            // Without this, nothing is highlighted when the picker opens
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
        }

        ringtoneLauncher.launch(intent)
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
        val ringtoneUri = addAlarmViewModel.selectedRingtoneUri.value

        addAlarmViewModel.scheduleAlarm(
            hour = hour,
            minute = minute,
            daysOfWeek = selectedDays,
            label = label,
            steps = steps,
            ringtoneUri = ringtoneUri
        )
    }

    private fun updateRingtoneLabel(uriString: String?) {
        val displayName = when {
            uriString == null -> getString(R.string.silent)
            else -> RingtoneManager.getRingtone(requireContext(), uriString.toUri())
                ?.getTitle(requireContext())
                ?: getString(R.string.default_alarm_sound)
        }
        binding.ringtoneEditText.setText(displayName)
    }

    /**
     * Cleans up the binding reference when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
