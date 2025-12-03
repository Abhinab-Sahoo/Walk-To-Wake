package com.example.stepcounter.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.data.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    val alarms: Flow<List<Alarm>> = alarmRepository.getAllAlarms()

    fun insert(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.insertAlarm(alarm)
        }
    }

    fun update(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }

}