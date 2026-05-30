package com.example.stepcounter.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.local.Alarm
import com.example.stepcounter.data.repository.AlarmRepository
import com.example.stepcounter.util.AlarmTimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    val alarms: Flow<List<Alarm>> = alarmRepository.getAllAlarms()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {

        viewModelScope.launch {

            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmRepository.updateAlarm(updatedAlarm)

            if (isEnabled) {
                val message = AlarmTimeHelper.getTimeUntilAlarm(
                    alarm.hour, alarm.minute, alarm.daysOfWeek
                )
                _toastMessage.emit(message)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarm)
        }
    }

}
