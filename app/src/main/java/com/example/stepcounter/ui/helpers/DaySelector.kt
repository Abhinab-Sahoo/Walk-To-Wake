package com.example.stepcounter.ui.helpers

import com.google.android.material.button.MaterialButton
import java.time.DayOfWeek

class DaySelector(
    private val buttons: Map<DayOfWeek, MaterialButton>
) {

    fun setSelectedDays(days: Set<DayOfWeek>) {
        buttons.values.forEach { it.isChecked = false }

        days.forEach { days ->
            buttons[days]?.isChecked = true
        }
    }

    fun getSelectedDays(): Set<DayOfWeek> {
        return buttons
            .filter { (_, button) -> button.isChecked }
            .keys
            .toSet()
    }
}