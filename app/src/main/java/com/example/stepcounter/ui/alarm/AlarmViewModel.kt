package com.example.stepcounter.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.data.AlarmRepository
import com.example.stepcounter.ui.add_alarm.AddAlarmUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _alarmScheduledEvent = MutableSharedFlow<AddAlarmUiEvent>()
    val alarmScheduledEvent = _alarmScheduledEvent.asSharedFlow()

    val alarms: Flow<List<Alarm>> = alarmRepository.getAllAlarms()

    fun schedule(
        hour: Int, minute: Int,
        daysOfWeek: Set<DayOfWeek>,
        label: String, steps: Int
    ) {
        viewModelScope.launch {

            val newAlarm = Alarm(
                hour = hour,
                minute = minute,
                daysOfWeek = daysOfWeek,
                isEnabled = true,
                label = label,
                steps = steps
            )

            alarmRepository.insertAlarm(newAlarm).toInt()

            _alarmScheduledEvent.emit(AddAlarmUiEvent.ShowToast("Alarm Scheduled!"))

        }
    }

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {

        viewModelScope.launch {

            val updatedAlarm = alarm.copy(isEnabled = isEnabled)
            alarmRepository.updateAlarm(updatedAlarm)

            val toastMessage = if (isEnabled) "${alarm.label} is ON" else "${alarm.label} is OFF"
            _alarmScheduledEvent.emit(AddAlarmUiEvent.ShowToast(toastMessage))
        }
    }

}
