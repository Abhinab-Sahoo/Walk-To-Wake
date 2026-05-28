package com.example.stepcounter.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object StepCounterState {

    // The running count of steps walked during this alarm
    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps

    // How many steps the user must walk to dismiss
    private val _requiredSteps = MutableStateFlow(0)
    val requiredSteps: StateFlow<Int> = _requiredSteps

    // The odometer reading at the start of this alarm
    // -1 means "not started yet"
    private var initialSteps = -1

    // Called once when a new alarm starts ringing
    // Resets everything back to zero for a fresh start
    fun reset(requiredSteps: Int) {
        _currentSteps.value = 0
        _requiredSteps.value = requiredSteps
        initialSteps = -1
    }

    // Called everytime the sensor reports a new reading
    // totalStepsSinceBoot = the raw odometer reading since boot
    fun updateSteps(totalStepsSinceBoot: Int) {
        if (initialSteps == -1) {
            initialSteps = totalStepsSinceBoot
        }
        _currentSteps.value = totalStepsSinceBoot - initialSteps
    }

    // Whether the target has been reached
    val isTargetReached: Boolean
        // get() means everytime you access it, it recalculates, no stale value.
        get() = _currentSteps.value >= _requiredSteps.value

}