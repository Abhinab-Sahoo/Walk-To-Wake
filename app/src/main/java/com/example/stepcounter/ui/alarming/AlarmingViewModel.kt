package com.example.stepcounter.ui.alarming

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.data.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmingViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _alarm = MutableStateFlow<Alarm?>(null)
    val alarm: StateFlow<Alarm?> = _alarm

    init {
        val alarmId: Int? = savedStateHandle["ALARM_ID"]

        if (alarmId != null && alarmId != -1) {
            viewModelScope.launch {
                val fetchedAlarm = alarmRepository.getAlarmById(alarmId)
                _alarm.value = fetchedAlarm
            }
        }
    }

    fun update(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }
}