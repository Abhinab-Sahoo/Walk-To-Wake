package com.example.stepcounter.ui.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.data.AlarmRepository
import com.example.stepcounter.ui.add_alarm.AddAlarmUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    /**
     * As it is a Shared Flow and used for emitting toast message for fragment
     * i am reusing it for edit alarm as well.
     */
    private val _alarmScheduledEvent = MutableSharedFlow<AddAlarmUiEvent>()
    val alarmScheduledEvent = _alarmScheduledEvent.asSharedFlow()

    private val _alarmToEdit = MutableStateFlow<Alarm?>(null)
    val alarmToEdit = _alarmToEdit.asStateFlow()

    private var currentAlarmId: Int = 0

    val alarms: Flow<List<Alarm>> = alarmRepository.getAllAlarms()

    fun initialize(alarm: Alarm?) {
        if (alarm != null) {
            currentAlarmId = alarm.id
            _alarmToEdit.value = alarm
        } else {
            currentAlarmId = 0
        }
    }

    fun schedule(
        hour: Int, minute: Int,
        daysOfWeek: Set<DayOfWeek>,
        label: String, steps: Int
    ) {
        viewModelScope.launch {

            val newAlarm = Alarm(
                id = currentAlarmId,
                hour = hour,
                minute = minute,
                daysOfWeek = daysOfWeek,
                isEnabled = true,
                label = label,
                steps = steps
            )

            if (currentAlarmId == 0) {
                alarmRepository.insertAlarm(newAlarm).toInt()
                _alarmScheduledEvent.emit(AddAlarmUiEvent.ShowToast("Alarm Scheduled!"))
            } else {
                alarmRepository.updateAlarm(newAlarm)
                _alarmScheduledEvent.emit(AddAlarmUiEvent.ShowToast("Alarm Updated!"))
            }
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
