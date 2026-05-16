package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.composeApp.components.timer.TimerViewState
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.INFINITE_TIMER_SETTINGS
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.TimerSettings
import dev.zhdanov.apps.shared.model.timer.InfiniteTimer
import dev.zhdanov.apps.shared.model.timer.PomodoroTimer
import dev.zhdanov.apps.shared.model.timer.Timer
import dev.zhdanov.apps.shared.model.timer.TimerStage
import dev.zhdanov.apps.shared.model.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface TimeProvider {
    fun nowEpochMilliseconds(): Long
}

@OptIn(ExperimentalTime::class)
object SystemTimeProvider : TimeProvider {
    override fun nowEpochMilliseconds(): Long = Clock.System.now().toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
class TimerSessionService(
    private val notificationService: NotificationService,
    private val focusSessionDataService: FocusSessionDataService,
    private val timerSettingsService: TimerSettingsService,
    private val focusTaskService: FocusTaskService,
    dispatchers: AppDispatchers,
    private val timeProvider: TimeProvider = SystemTimeProvider
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val _time = MutableStateFlow(0)
    private val _progress = MutableStateFlow(0f)
    private val _state = MutableStateFlow(TimerViewState.WORK)
    private val _isRunning = MutableStateFlow(false)
    private val _isPause = MutableStateFlow(false)
    private val _settings = MutableStateFlow(DEFAULT_TIMER_SETTINGS)
    private val _lastPartDuration = MutableStateFlow(0)

    private val _focusSessionStart = MutableStateFlow<Long?>(null)
    private val _totalPauseTime = MutableStateFlow(0)
    private var pauseStartTime: Long? = null

    private val timerListener: Timer.TimerListener = object : Timer.TimerListener {
        override fun onStart(stage: TimerStage) {
            _isRunning.value = true
            if (stage == TimerStage.WORK && _focusSessionStart.value == null) {
                _focusSessionStart.value = timeProvider.nowEpochMilliseconds()
            }
        }

        override fun onFinish(old: TimerStage, new: TimerStage, duration: Int) {
            _isRunning.value = false

            coroutineScope.launch {
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

            if (isPaused) {
                pauseStartTime = timeProvider.nowEpochMilliseconds()
            } else {
                pauseStartTime?.let { pauseStart ->
                    val pauseDuration = ((timeProvider.nowEpochMilliseconds() - pauseStart) / 1000).toInt()
                    _totalPauseTime.value += pauseDuration
                }
                pauseStartTime = null
            }
        }

        override fun onTick(time: Int, progress: Float) {
            _time.value = time
            _progress.value = progress
        }

        override fun onChangeState(state: TimerState) {
            _isRunning.value = when (state) {
                TimerState.IN_PROGRESS, TimerState.PAUSE -> true
                else -> false
            }
        }
    }

    private val _timer = MutableStateFlow<Timer>(createTimer(_settings.value))

    val time = _time.map(this::formatTime)
    val progress = _progress.asStateFlow()
    val isRunning = _isRunning.asStateFlow()
    val isPause = _isPause.asStateFlow()
    val settings = _settings.asStateFlow()
    val state = _state.asStateFlow()
    val lastPartDuration = _lastPartDuration.asStateFlow()
    val settingList = timerSettingsService.timerSettings.asStateFlow()
        .map { listOf(INFINITE_TIMER_SETTINGS) + it }

    init {
        timerSettingsService.timerSettings
            .onEach { loadDefaultTimer() }
            .launchIn(coroutineScope)
    }

    fun startTimer() {
        _timer.value.start()
    }

    suspend fun saveFeedback(feedback: CreateFocusTime) {
        val pauseTime = calculatePauseTime()
        val taskIds = focusTaskService.getAllTaskIdsForSession()

        focusSessionDataService.addFocusTimeWithTasks(
            duration = feedback.duration.toLong(),
            feedback = feedback.feedback,
            finishedAt = feedback.finishedAt,
            startedAt = _focusSessionStart.value,
            pauseTime = pauseTime?.toLong(),
            taskIds = taskIds
        )

        focusTaskService.resetSession()
        resetFocusTracking()
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

    private fun loadDefaultTimer() {
        val defaultSetting = timerSettingsService.loadDefaultTimerSetting()
        if (defaultSetting != null) {
            _settings.value = defaultSetting
            _timer.value = createTimer(defaultSetting)
        }
    }

    private fun calculatePauseTime(): Int? {
        val focusStart = _focusSessionStart.value ?: return null
        val totalElapsedSeconds = (timeProvider.nowEpochMilliseconds() - focusStart) / 1000
        val timerDuration = _lastPartDuration.value
        val calculatedPause = (totalElapsedSeconds - timerDuration).toInt().coerceAtLeast(0)

        return maxOf(_totalPauseTime.value, calculatedPause).takeIf { it > 0 }
    }

    private fun resetFocusTracking() {
        _focusSessionStart.value = null
        _totalPauseTime.value = 0
        pauseStartTime = null
    }

    private fun createTimer(settings: TimerSettings) = if (settings.isInfinite) {
        InfiniteTimer(
            coroutineScope,
            when (_state.value) {
                TimerViewState.WORK -> TimerStage.WORK
                else -> TimerStage.REST
            },
            timerListener
        )
    } else {
        PomodoroTimer(
            settings,
            coroutineScope,
            when (_state.value) {
                TimerViewState.WORK -> TimerStage.WORK
                else -> TimerStage.REST
            },
            timerListener
        )
    }

    private fun formatTime(value: Int): String {
        val minutes = value / 60
        val seconds = value % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
