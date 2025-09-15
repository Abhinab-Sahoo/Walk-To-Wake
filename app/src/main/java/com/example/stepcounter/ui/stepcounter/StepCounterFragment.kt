package com.example.stepcounter.ui.stepcounter

import android.content.Context.SENSOR_SERVICE
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.stepcounter.R
import com.example.stepcounter.databinding.FragmentStepCounterBinding


class StepCounterFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentStepCounterBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = 0
    private var stepsStarted: Boolean = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startStepCounting()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentStepCounterBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            binding.stepCount.text = getString(R.string.sensor_not_available)
        }
        checkAndRequestPermission()

        goToAlarmScreen()
    }

    private fun goToAlarmScreen() {
        binding.alarmBtn.setOnClickListener {
            findNavController().navigate(R.id.action_stepCounterFragment_to_alarmFragment)
        }
    }

    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startStepCounting()
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            startStepCounting()
        }
    }

    private fun startStepCounting() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            binding.stepCount.text = getString(R.string.sensor_not_available)
        }
    }

    override fun onResume() {
        super.onResume()
        if (stepSensor != null &&
            checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED ) {

            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)

        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("StepCounterFragment", "onAccuracyChanged: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()

            if (!stepsStarted) {
                initialSteps = totalStepsSinceBoot
                stepsStarted = true
            }

            val currentSteps = totalStepsSinceBoot - initialSteps
            binding.stepCount.text = getString(R.string.steps, currentSteps)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}