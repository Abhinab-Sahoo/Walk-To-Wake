package com.example.stepcounter.ui.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepcounter.R
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.databinding.FragmentAlarmBinding
import java.time.DayOfWeek


class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private lateinit var alarmAdapter: AlarmAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAlarmBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addAlarmFab.setOnClickListener {
            findNavController().navigate(R.id.action_alarmFragment_to_addAlarmFragment)
        }

        alarmAdapter = AlarmAdapter(
            clickListener = { alarm ->
                Toast.makeText(
                    requireContext(),
                    "Clicked on ${alarm.label}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            switchClickListener = { alarm, isChecked ->
                Toast.makeText(
                    requireContext(),
                    "${alarm.label} is now ${if (isChecked) "On" else "Off"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        binding.recyclerView.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val dummyAlarms = listOf(
            Alarm(id = 1, hour = 8, minute = 30, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), isEnabled = true, label = "Morning Work", steps = 100),
            Alarm(id = 2, hour = 22, minute = 0, daysOfWeek = emptySet(), isEnabled = false, label = "Bedtime", steps = 0),
            Alarm(id = 3, hour = 18, minute = 45, daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), isEnabled = true, label = "Weekend", steps = 500)
        )
        alarmAdapter.submitList(dummyAlarms)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}