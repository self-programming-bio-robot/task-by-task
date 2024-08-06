package dev.zhdanov.apps.composeApp.components.timer

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.TEST_TIMER_SETTINGS
import dev.zhdanov.apps.shared.TimerSettings
import dev.zhdanov.apps.shared.TimerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.math.max

class TimerViewModel(
    private val notificationService: NotificationService
) : ViewModel() {
    private val _time = MutableStateFlow(0)
    private val _workPeriodCounter = mutableStateOf(0)
    private val _state = MutableStateFlow(TimerState.WORK)
    private val _isRunning = MutableStateFlow(false)
    private val _isPause = MutableStateFlow(false)
    private val _settings = MutableStateFlow(DEFAULT_TIMER_SETTINGS)

    val time = _time.map(this::getTime)
    val isRunning = _isRunning.asStateFlow()
    val isPause = _isPause.asStateFlow()
    val settings = _settings.asStateFlow()
    val state = _state.asStateFlow()

    init {
        _time.value = _settings.value.workDuration
        _state.value = TimerState.WORK
        updateCurrentTimer()
    }

    fun startTimer() {
        val settings = this._settings.value
        this._isRunning.value = true

        viewModelScope.launch {
            while (_time.value > 0 && _isRunning.value) {
                if (!_isPause.value) {
                    if (_isRunning.value) {
                        _time.value = max(0, _time.value - 1)
                    }
                    delay(1000)
                } else {
                    delay(100)
                }
            }
            if (_isRunning.value) {
                _isRunning.value = false

                notificationService.addNotification(Notification("Finish ${_state.value}"))

                nextState()
            }
        }
    }

    fun stopTimer() {
        this._isRunning.value = false
        updateCurrentTimer()
    }

    fun pauseTimer() {
        _isPause.value = !_isPause.value
    }

    fun skipTimer() {
        _time.value = 0
    }

    private fun nextState() {
        val settings = this._settings.value
        if (_state.value == TimerState.WORK) {
            _workPeriodCounter.value++
            if (_workPeriodCounter.value >= settings.workCycles) {
                _workPeriodCounter.value = 0
                _state.value = TimerState.LONG_BREAK
            } else {
                _state.value = TimerState.BREAK
            }
        } else {
            _state.value = TimerState.WORK
        }

        updateCurrentTimer()
    }

    private fun getTime(value: Int): String {
        val minutes = value / 60
        val seconds = value % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    private fun updateCurrentTimer() {
        val settings = this._settings.value
        when (_state.value) {
            TimerState.WORK -> {
                _time.value = settings.workDuration
            }

            TimerState.BREAK -> {
                _time.value = settings.shortBreakDuration

            }

            TimerState.LONG_BREAK -> {
                _time.value = settings.longBreakDuration
            }
        }
    }
}