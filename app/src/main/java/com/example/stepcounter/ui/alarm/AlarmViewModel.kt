package com.example.stepcounter.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.data.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    val alarms: Flow<List<Alarm>> = alarmRepository.getAllAlarms()

    suspend fun insert(alarm: Alarm): Long {
        val deferredId = viewModelScope.async {
            alarmRepository.insertAlarm(alarm)
        }
        return deferredId.await()
    }

    fun update(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }
}
