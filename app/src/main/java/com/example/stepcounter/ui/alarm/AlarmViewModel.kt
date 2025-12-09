package com.example.stepcounter.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.data.AlarmRepository
import com.example.stepcounter.receiver.AlarmReceiver
import com.example.stepcounter.ui.add_alarm.AddAlarmUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    @ApplicationContext private val context: Context
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
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val newAlarm = Alarm(
                hour = hour,
                minute = minute,
                daysOfWeek = daysOfWeek,
                isEnabled = true,
                label = label,
                steps = steps
            )

            val alarmId = alarmRepository.insertAlarm(newAlarm).toInt()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (daysOfWeek.isEmpty() &&
                calendar.timeInMillis <= System.currentTimeMillis()
            ) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarmId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            _alarmScheduledEvent.emit(AddAlarmUiEvent.ShowToast("Alarm Scheduled!"))

        }
    }

    fun update(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }
}
