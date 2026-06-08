package com.example.stepcounter.ui.alarm

import com.example.stepcounter.data.local.Alarm

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowDeleteUndo(val alarm: Alarm) : UiEvent()
}