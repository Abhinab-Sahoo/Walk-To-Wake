package com.example.stepcounter.ui.alarm

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stepcounter.R
import com.example.stepcounter.data.local.Alarm
import com.example.stepcounter.databinding.FragmentAlarmBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private val alarmListViewModel: AlarmListViewModel by viewModels()

    private val alarmAdapter = AlarmAdapter(
        clickListener = { alarm ->
            onAlarmClicked(alarm)
        },
        switchClickListener = { alarm, isChecked ->
            onAlarmToggled(alarm, isChecked)
        }
    )

    private var arrowAnimator: ObjectAnimator? = null

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

        setupSwipeToDelete()
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
        alarmListViewModel.toggleAlarm(alarm, isChecked)
    }

    private fun observeUiEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmListViewModel.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.ShowToast -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT ).show()
                        }

                        is UiEvent.ShowDeleteUndo -> {
                            Snackbar.make(binding.root, "Alarm deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo") { alarmListViewModel.undoDelete(event.alarm) }
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun observeAlarms() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                alarmListViewModel.alarms.collect { alarms ->
                    alarmAdapter.submitList(alarms)

                    if (alarms.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                    }
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyStateView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        // Create animation only if not already running
        if (arrowAnimator == null) {
            arrowAnimator = ObjectAnimator.ofFloat(binding.ivArrow, "translationY", 0f, 25f).apply {
                duration = 1000 // 1 second up, 1 second down
                repeatMode = ObjectAnimator.REVERSE
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator() // Makes it smooth
            }
            arrowAnimator?.start()
        }
    }

    private fun hideEmptyState() {
        binding.emptyStateView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE

        // Stop animation to save battery
        arrowAnimator?.cancel()
        arrowAnimator = null
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

    private fun setupSwipeToDelete() {
        val callback = SwipeToDeleteCallback(requireContext()) { position ->
            val alarm = alarmAdapter.currentList[position]
            alarmListViewModel.deleteAlarm(alarm)
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.recyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}