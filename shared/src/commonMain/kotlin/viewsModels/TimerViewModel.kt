package viewsModels

import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import models.TimerSettings
import services.Timer
import kotlin.time.Duration

data class TimerState(
    val started: Instant? = null,
    val duration: Duration,
    val reminders: Duration,
    val onDone: (() -> Unit)? = null,
    val condition: TimerCondition = TimerCondition.WAIT_WORK,
    val workInLine: Int = 1,
)

enum class TimerCondition {
    WAIT_WORK,
    WORKING,
    WAIT_SHORT_REST,
    SHORT_REST,
    WAIT_LONG_REST,
    LONG_REST,
    ;
}

class TimerViewModel(
    val timerSettings: TimerSettings = TimerSettings(),
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
            val newCondition = updateCondition(it)
            it.copy(
                started = Clock.System.now(),
                duration = duration(newCondition),
                reminders = duration(newCondition),
                condition = newCondition
            )
        }
    }

    override fun updateTime(time: Instant): Boolean {
        var done = false
        _uiState.update {
            val new = it.started?.let { started ->
                it.copy(
                    reminders = it.duration - time.minus(started)
                )
            } ?: it

            if (new.reminders <= Duration.ZERO) {
                new.onDone?.let { it() }
                done = true
                val newCondition = updateCondition(new)
                new.copy(
                    started = null,
                    duration = duration(newCondition),
                    reminders = duration(newCondition),
                    condition = newCondition,
                    workInLine = if (new.condition == TimerCondition.WORKING) {
                        new.workInLine + 1
                    }
                    else {
                        new.workInLine
                    }
                )
            } else {
                new
            }
        }

        return done
    }

    private fun duration(newCondition: TimerCondition) = when (newCondition) {
        TimerCondition.WAIT_SHORT_REST, TimerCondition.SHORT_REST -> timerSettings.shortRestDuration
        TimerCondition.WAIT_LONG_REST, TimerCondition.LONG_REST -> timerSettings.longRestDuration
        else -> timerSettings.baseDuration
    }

    private fun updateCondition(state: TimerState): TimerCondition {
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
}