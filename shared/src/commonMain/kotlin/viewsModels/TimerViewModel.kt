package viewsModels

import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toDateTimePeriod
import models.Pomodoro
import models.TimerSettings
import models.TodoItem
import services.Timer
import services.TodoService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TimerState(
    val started: Instant? = null,
    val duration: Duration,
    val reminders: Duration,
    val onDone: (() -> Unit)? = null,
    val condition: TimerCondition = TimerCondition.WAIT_WORK,
    val workInLine: Int = 1,
    val focusTodo: TodoItem? = null
) {
    val formattedString = reminders.toDateTimePeriod().let { time ->
        val minutes = if (time.minutes < 10) {
            "0${time.minutes}"
        } else {
            time.minutes.toString()
        }
        val seconds = if (time.seconds < 10) {
            "0${time.seconds}"
        } else {
            time.seconds.toString()
        }

        "$minutes:$seconds"
    }
}

enum class TimerCondition(private val active: Boolean) {
    WAIT_WORK(false),
    WORKING(true),
    WAIT_SHORT_REST(false),
    SHORT_REST(true),
    WAIT_LONG_REST(false),
    LONG_REST(true),
    ;

    fun isActive(): Boolean = this.active
}

class TimerViewModel(
    val timerSettings: TimerSettings = TimerSettings(),
    val todoService: TodoService
) : ViewModel(), Timer {
    private val _uiState = MutableStateFlow(
        TimerState(
            duration = timerSettings.baseDuration,
            reminders = timerSettings.baseDuration
        )
    )
    val uiState = _uiState.asStateFlow()

    fun start() {
        _uiState.update {
            val newCondition = nextCondition(it)
            it.copy(
                started = Clock.System.now(),
                duration = duration(newCondition),
                reminders = duration(newCondition),
                condition = newCondition
            )
        }
        viewModelScope.launch {
            var done = false
            while (!done) {
                done = updateTime(Clock.System.now())
                delay(1.seconds)
            }
        }
    }

    fun stop() {
        _uiState.update { state ->
            val newCondition = nextCondition(state)
            if (!newCondition.isActive()) {
                var focusTodo = state.focusTodo
                if (state.condition == TimerCondition.WORKING) {
                    focusTodo = state.focusTodo?.let { focusTodo ->
                        finishWorking(focusTodo, state.duration, state.started!!, false)
                    }
                }

                state.copy(
                    started = null,
                    focusTodo = focusTodo,
                    duration = duration(newCondition),
                    reminders = duration(newCondition),
                    condition = newCondition
                )
            } else {
                state
            }
        }
    }

    override fun updateTime(time: Instant): Boolean {
        var done = false
        _uiState.update { state ->
            val new = state.started?.let { started ->
                state.copy(
                    reminders = state.duration - time.minus(started)
                )
            } ?: state

            if (new.reminders <= Duration.ZERO) {
                var focusTodo = new.focusTodo
                if (new.condition == TimerCondition.WORKING) {
                    focusTodo = new.focusTodo?.let { focusTodo ->
                        finishWorking(focusTodo, new.duration, new.started!!, true)
                    }
                }
                new.onDone?.let { it() }
                done = true
                val newCondition = nextCondition(new)
                new.copy(
                    started = null,
                    duration = duration(newCondition),
                    reminders = duration(newCondition),
                    condition = newCondition,
                    focusTodo = focusTodo,
                    workInLine = if (new.condition == TimerCondition.WORKING) {
                        new.workInLine + 1
                    } else {
                        new.workInLine
                    }
                )
            } else {
                new
            }
        }

        return done
    }

    private fun finishWorking(
        todo: TodoItem,
        duration: Duration,
        start: Instant,
        isDone: Boolean
    ): TodoItem {
        val pomodoro = Pomodoro(duration, start, isDone)
        return todoService.addPomodoro(todo.id, pomodoro) ?: TODO("do something")
    }

    private fun duration(newCondition: TimerCondition) = when (newCondition) {
        TimerCondition.WAIT_SHORT_REST, TimerCondition.SHORT_REST -> timerSettings.shortRestDuration
        TimerCondition.WAIT_LONG_REST, TimerCondition.LONG_REST -> timerSettings.longRestDuration
        else -> timerSettings.baseDuration
    }

    private fun nextCondition(state: TimerState): TimerCondition {
        return when (state.condition) {
            TimerCondition.WORKING -> {
                if (state.workInLine % timerSettings.sequenceWorkingLength == 0) {
                    TimerCondition.WAIT_LONG_REST
                } else {
                    TimerCondition.WAIT_SHORT_REST
                }
            }

            TimerCondition.SHORT_REST, TimerCondition.LONG_REST -> TimerCondition.WAIT_WORK
            TimerCondition.WAIT_WORK -> TimerCondition.WORKING
            TimerCondition.WAIT_SHORT_REST -> TimerCondition.SHORT_REST
            TimerCondition.WAIT_LONG_REST -> TimerCondition.LONG_REST
        }
    }

    override fun onDone(callback: () -> Unit) {
        _uiState.update {
            it.copy(
                onDone = callback
            )
        }
    }

    fun setFocusTodo(todo: TodoItem) {
        _uiState.update {
            it.copy(
                focusTodo = todo
            )
        }
    }

    fun clearFocusTodo() {
        _uiState.update {
            it.copy(
                focusTodo = null
            )
        }
    }
}