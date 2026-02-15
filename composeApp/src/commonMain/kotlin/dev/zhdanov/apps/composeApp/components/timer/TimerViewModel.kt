package dev.zhdanov.apps.composeApp.components.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.INFINITE_TIMER_SETTINGS
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.TimerSettings
import dev.zhdanov.apps.shared.model.timer.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TimerViewModel(
    private val notificationService: NotificationService,
    private val database: Database,
    private val timerSettingsService: TimerSettingsService,
    private val focusTaskService: FocusTaskService,
) : ViewModel() {
    private val _time = MutableStateFlow(0)
    private val _progress = MutableStateFlow(0f)
    private val _state = MutableStateFlow(TimerViewState.WORK)
    private val _isRunning = MutableStateFlow(false)
    private val _isPause = MutableStateFlow(false)
    private val _settings = MutableStateFlow(DEFAULT_TIMER_SETTINGS)
    private val _lastPartDuration = MutableStateFlow(0)

    // Track focus session start time
    private val _focusSessionStart = MutableStateFlow<Long?>(null)

    // Track cumulative pause time
    private val _totalPauseTime = MutableStateFlow(0)

    // Track when pause began for calculating pause duration
    private var _pauseStartTime: Long? = null

    // Initialize timerListener BEFORE init block
    private val timerListener: Timer.TimerListener = object : Timer.TimerListener {
        override fun onStart(stage: TimerStage) {
            _isRunning.value = true

            // Track focus session start when work stage begins
            if (stage == TimerStage.WORK && _focusSessionStart.value == null) {
                _focusSessionStart.value = Clock.System.now().toEpochMilliseconds()
            }
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

            // Track pause time accumulation
            if (isPaused) {
                // Pause started - record timestamp
                _pauseStartTime = Clock.System.now().toEpochMilliseconds()
            } else {
                // Pause ended - calculate duration and add to total
                _pauseStartTime?.let { pauseStart ->
                    val pauseDuration = ((Clock.System.now().toEpochMilliseconds() - pauseStart) / 1000).toInt()
                    _totalPauseTime.value += pauseDuration
                }
                _pauseStartTime = null
            }
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

    init {
        // Observe timer settings and load default when available
        timerSettingsService.timerSettings
            .onEach { loadDefaultTimer() }
            .launchIn(viewModelScope)
    }

    private fun loadDefaultTimer() {
        val defaultSetting = timerSettingsService.loadDefaultTimerSetting()
        if (defaultSetting != null) {
            _settings.value = defaultSetting
            _timer.value = createTimer(defaultSetting)
        }
    }

    val time = _time.map(this::getTime)
    val progress = _progress.asStateFlow()
    val isRunning = _isRunning.asStateFlow()
    val isPause = _isPause.asStateFlow()
    val settings = _settings.asStateFlow()
    val state = _state.asStateFlow()
    val lastPartDuration = _lastPartDuration.asStateFlow()
    val settingList = timerSettingsService.timerSettings.asStateFlow()
        .map { listOf(INFINITE_TIMER_SETTINGS) + it }

    fun startTimer() {
        _timer.value.start()
    }

    fun saveFeedback(feedback: CreateFocusTime) {
        viewModelScope.launch {
            val pauseTime = calculatePauseTime()
            val selectedTaskIds = focusTaskService.getSelectedTaskIds()

            database.addFocusTimeWithTasks(
                duration = feedback.duration.toLong(),
                feedback = feedback.feedback,
                finishedAt = feedback.finishedAt,
                startedAt = _focusSessionStart.value,
                pauseTime = pauseTime?.toLong(),
                taskIds = selectedTaskIds
            )

            // Clear selected tasks after saving
            focusTaskService.clearSelectedTasks()
            resetFocusTracking()
        }
    }

    private fun calculatePauseTime(): Int? {
        val focusStart = _focusSessionStart.value ?: return null
        val now = Clock.System.now().toEpochMilliseconds()
        val totalElapsedSeconds = (now - focusStart) / 1000
        val timerDuration = _lastPartDuration.value

        // Calculate pause time: total elapsed - active timer time
        val calculatedPause = (totalElapsedSeconds - timerDuration).toInt().coerceAtLeast(0)

        // Use the maximum of tracked pauses and calculated value for accuracy
        return maxOf(_totalPauseTime.value, calculatedPause).takeIf { it > 0 }
    }

    private fun resetFocusTracking() {
        _focusSessionStart.value = null
        _totalPauseTime.value = 0
        _pauseStartTime = null
    }

    fun closeFeedback() {
        when (_timer.value.getStage()) {
            TimerStage.WORK -> _state.value = TimerViewState.WORK
            TimerStage.REST -> _state.value = TimerViewState.BREAK
        }
    }

    fun stopTimer() {
        _timer.value.reset()
        resetFocusTracking()
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
