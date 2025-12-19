package com.example.stepcounter.ui.alarm

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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stepcounter.R
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.databinding.FragmentAlarmBinding
import com.example.stepcounter.ui.add_alarm.AddAlarmUiEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private val alarmViewModel: AlarmViewModel by viewModels()

    private val alarmAdapter = AlarmAdapter(
        clickListener = { alarm ->
            onAlarmClicked(alarm)
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        observeUiEvents()
        observeAlarms()
        setupFab()
        setupRecyclerView()

    }

    private fun onAlarmClicked(alarm: Alarm) {
        findNavController().navigate(
            AlarmFragmentDirections.alarmFragmentToAddAlarmFragment(alarm)
        )
    }

    private fun onAlarmToggled(alarm: Alarm, isChecked: Boolean) {
        alarmViewModel.toggleAlarm(alarm, isChecked)
    }

    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmViewModel.alarmScheduledEvent.collect { event ->
                    if (event is AddAlarmUiEvent.ShowToast) {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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
            findNavController().navigate(R.id.alarmFragment_to_addAlarmFragment)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private val swipeHandler = object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val alarm = alarmAdapter.currentList[position]

            when (direction) {
                ItemTouchHelper.LEFT -> {
                    alarmViewModel.deleteAlarm(alarm)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}