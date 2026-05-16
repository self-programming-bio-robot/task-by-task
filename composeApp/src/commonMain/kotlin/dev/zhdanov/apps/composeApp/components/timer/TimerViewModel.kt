package dev.zhdanov.apps.composeApp.components.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.TimerSessionService
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.launch

class TimerViewModel(
    private val timerSessionService: TimerSessionService
) : ViewModel() {
    val time = timerSessionService.time
    val progress = timerSessionService.progress
    val isRunning = timerSessionService.isRunning
    val isPause = timerSessionService.isPause
    val settings = timerSessionService.settings
    val state = timerSessionService.state
    val lastPartDuration = timerSessionService.lastPartDuration
    val settingList = timerSessionService.settingList

    fun startTimer() {
        timerSessionService.startTimer()
    }

    fun saveFeedback(feedback: CreateFocusTime) {
        viewModelScope.launch {
            timerSessionService.saveFeedback(feedback)
        }
    }

    fun closeFeedback() {
        timerSessionService.closeFeedback()
    }

    fun stopTimer() {
        timerSessionService.stopTimer()
    }

    fun pauseTimer() {
        timerSessionService.pauseTimer()
    }

    fun skipTimer() {
        timerSessionService.skipTimer()
    }

    fun changeTimerSettings(settings: TimerSettings) {
        timerSessionService.changeTimerSettings(settings)
    }
}

enum class TimerViewState {
    WORK,
    BREAK,
    FEEDBACK
}
