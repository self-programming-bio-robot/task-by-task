package dev.zhdanov.apps.composeApp.components.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.INFINITE_TIMER_SETTINGS
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.TimerSettings
import dev.zhdanov.apps.shared.model.timer.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TimerViewModel(
    private val notificationService: NotificationService,
    private val database: Database,
    private val timerSettingsService: TimerSettingsService,
) : ViewModel() {
    private val _time = MutableStateFlow(0)
    private val _progress = MutableStateFlow(0f)
    private val _state = MutableStateFlow(TimerViewState.WORK)
    private val _isRunning = MutableStateFlow(false)
    private val _isPause = MutableStateFlow(false)
    private val _settings = MutableStateFlow(DEFAULT_TIMER_SETTINGS)
    private val _lastPartDuration = MutableStateFlow(0)

    val time = _time.map(this::getTime)
    val progress = _progress.asStateFlow()
    val isRunning = _isRunning.asStateFlow()
    val isPause = _isPause.asStateFlow()
    val settings = _settings.asStateFlow()
    val state = _state.asStateFlow()
    val lastPartDuration = _lastPartDuration.asStateFlow()
    val settingList = timerSettingsService.timerSettings.asStateFlow()
        .map { listOf(INFINITE_TIMER_SETTINGS) + it }

    private val timerListener: Timer.TimerListener = object : Timer.TimerListener {
        override fun onStart(stage: TimerStage) {
            _isRunning.value = true
        }

        override fun onFinish(old: TimerStage, new: TimerStage, duration: Int) {
            _isRunning.value = false

            viewModelScope.launch {
                notificationService.addNotification(
                    Notification("Finish ${old.name.lowercase()}")
                )
            }

            when (old) {
                TimerStage.WORK -> {
                    _state.value = TimerViewState.FEEDBACK
                    _lastPartDuration.value = duration
                }
                TimerStage.REST -> when (new) {
                    TimerStage.WORK -> _state.value = TimerViewState.WORK
                    TimerStage.REST -> _state.value = TimerViewState.BREAK
                }
            }
        }

        override fun onPause(isPaused: Boolean) {
            _isPause.value = isPaused
        }

        override fun onTick(time: Int, progress: Float) {
            _time.value = time
            _progress.value = progress
        }

        override fun onChangeState(state: TimerState) {
            when (state) {
                TimerState.IN_PROGRESS, TimerState.PAUSE -> _isRunning.value = true
                else -> _isRunning.value = false
            }
        }
    }

    private val _timer = MutableStateFlow<Timer>(createTimer(_settings.value))

    fun startTimer() {
        _timer.value.start()
    }

    fun saveFeedback(feedback: CreateFocusTime) {
        viewModelScope.launch {
            database.addFocusTime(feedback)
        }
    }

    fun closeFeedback() {
        when (_timer.value.getStage()) {
            TimerStage.WORK -> _state.value = TimerViewState.WORK
            TimerStage.REST -> _state.value = TimerViewState.BREAK
        }
    }

    fun stopTimer() {
        _timer.value.reset()
    }

    fun pauseTimer() {
        _timer.value.pause()
    }

    fun skipTimer() {
        _timer.value.stop()
    }

    fun changeTimerSettings(settings: TimerSettings) {
        _settings.value = settings
        _timer.value = createTimer(settings)
    }

    private fun createTimer(settings: TimerSettings) = if (settings.isInfinite) {
        InfiniteTimer(
            viewModelScope,
            when (_state.value) {
                TimerViewState.WORK -> TimerStage.WORK
                else -> TimerStage.REST
            },
            timerListener
        )
    } else {
        PomodoroTimer(
            settings,
            viewModelScope,
            when (_state.value) {
                TimerViewState.WORK -> TimerStage.WORK
                else -> TimerStage.REST
            },
            timerListener
        )
    }

    private fun getTime(value: Int): String {
        val minutes = value / 60
        val seconds = value % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

}

enum class TimerViewState {
    WORK,
    BREAK,
    FEEDBACK
}
