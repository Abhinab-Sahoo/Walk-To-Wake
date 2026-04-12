package com.example.stepcounter.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object StepCounterState {

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps

    private val _requiredSteps = MutableStateFlow(0)
    val requiredSteps: StateFlow<Int> = _requiredSteps

    private var initialSteps = -1

    fun reset(requiredSteps: Int) {
        _currentSteps.value = 0
        _requiredSteps.value = requiredSteps
        initialSteps = -1
    }

    fun updateSteps(totalStepsSinceBoot: Int) {
        if (initialSteps == -1) {
            initialSteps = totalStepsSinceBoot
        }
        _currentSteps.value = totalStepsSinceBoot - initialSteps
    }

    val isTargetReached: Boolean
        get() = _currentSteps.value >= _requiredSteps.value

}