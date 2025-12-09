package com.example.stepcounter.ui.add_alarm

sealed class AddAlarmUiEvent {
    data class ShowToast( val message: String) : AddAlarmUiEvent()
}