package com.example.stepcounter.ui.add_alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.local.Alarm
import com.example.stepcounter.data.repository.AlarmRepository
import com.example.stepcounter.util.AlarmTimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject


@HiltViewModel
class AddAlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _alarmScheduledEvent = MutableSharedFlow<AddAlarmUiEvent>()
    val alarmScheduledEvent = _alarmScheduledEvent.asSharedFlow()

    private val _alarmToEdit = MutableStateFlow<Alarm?>(null)
    val alarmToEdit = _alarmToEdit.asStateFlow()

    private var currentAlarmId: Int = 0

    fun initialize(alarm: Alarm?) {
        if (alarm != null) {
            currentAlarmId = alarm.id
            _alarmToEdit.value = alarm
        } else {
            currentAlarmId = 0
        }
    }

    fun scheduleAlarm(
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
            } else {
                alarmRepository.updateAlarm(newAlarm)
            }

            val message = AlarmTimeHelper.getTimeUntilAlarm(hour, minute, daysOfWeek)
            _alarmScheduledEvent.emit(AddAlarmUiEvent.ShowToast(message))
        }
    }
}