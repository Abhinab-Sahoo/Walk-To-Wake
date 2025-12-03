package com.example.stepcounter.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepcounter.R
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.databinding.FragmentAlarmBinding
import com.example.stepcounter.receiver.AlarmReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!


    private val alarmViewModel: AlarmViewModel by viewModels()
    private lateinit var alarmManager: AlarmManager

    private val alarmAdapter = AlarmAdapter(
        clickListener = { alarm ->
            Toast.makeText(requireContext(), "Clicked on ${alarm.label}", Toast.LENGTH_SHORT).show()
        },
        switchClickListener = { alarm, isChecked ->
            onAlarmToggled(alarm, isChecked)
        }
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAlarmBinding.inflate(layoutInflater, container, false)
        alarmManager = requireContext().getSystemService(AlarmManager::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAlarms()
        setupFab()
        setupRecyclerView()

    }

    private fun onAlarmToggled(
        alarm: Alarm,
        isChecked: Boolean
    ) {
        val updatedAlarm = alarm.copy(isEnabled = isChecked)
        alarmViewModel.update(updatedAlarm)

        if (isChecked) {
            scheduleSystemAlarm(updatedAlarm)
            Toast.makeText(requireContext(), "${alarm.label} is ON", Toast.LENGTH_SHORT).show()
        } else {
            cancelSystemAlarm(updatedAlarm)
            Toast.makeText(requireContext(), "${alarm.label} is OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleSystemAlarm(alarm: Alarm) {

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
        }

        if (alarm.daysOfWeek.isEmpty() && calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarmIntent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.creationTimeInMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            alarm.creationTimeInMillis.toInt(),
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelSystemAlarm(alarm: Alarm) {

        val alarmIntent = Intent(requireContext(), AlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            alarm.creationTimeInMillis.toInt(),
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun observeAlarms() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmViewModel.alarms.collect { alarms ->
                    alarmAdapter.submitList(alarms)
                }
            }
        }
    }

    private fun setupFab() {
        binding.addAlarmFab.setOnClickListener {
            findNavController().navigate(R.id.action_alarmFragment_to_addAlarmFragment)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}