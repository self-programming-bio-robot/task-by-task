package dev.zhdanov.apps.composeApp.components.settings.timers.editor

import androidx.lifecycle.ViewModel
import dev.zhdanov.apps.composeApp.services.TimerSettingsService

class EditableTimerSettingsViewModel(
    private val service: TimerSettingsService,
) : ViewModel() {

    fun updateTimerSettings(
        id: Long?,
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        workCycles: Int,
    ) {
        if (id == null) {
            service.createTimerSetting(
                workDuration = workDuration,
                shortBreakDuration = shortBreakDuration,
                longBreakDuration = longBreakDuration,
                workCycles = workCycles,
            )
        } else {
            service.updateTimerSetting(
                id = id,
                workDuration = workDuration,
                shortBreakDuration = shortBreakDuration,
                longBreakDuration = longBreakDuration,
                workCycles = workCycles,
            )
        }
    }

}
