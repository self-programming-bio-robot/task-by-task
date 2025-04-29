package dev.zhdanov.apps.shared.model.timer

import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.*

class PomodoroTimer(
    private val settings: TimerSettings,
    private val coroutineScope: CoroutineScope,
    initialStage: TimerStage,
    override val timerListener: Timer.TimerListener
) : Timer {

    private var stage: PomodoroTimerStage = when (initialStage) {
        TimerStage.WORK -> PomodoroTimerStage.WORK
        TimerStage.REST -> PomodoroTimerStage.SHORT_BREAK
    }
    private var state: TimerState = TimerState.IDLE
    private var time: Int = setInitialTime()
    private var totalTime: Int = time
    private var cycles: Int = 0
    private var timerJob: Job? = null

    override fun start() {
        if (state == TimerState.IN_PROGRESS) return
        if (state == TimerState.IDLE) {
            timerListener.onStart(stage.mappingStage)
        }
        state = TimerState.IN_PROGRESS
        timerListener.onChangeState(state)

        timerJob = coroutineScope.launch {
            while (this.isActive && time > 0 && state == TimerState.IN_PROGRESS) {
                delay(1000)
                time -= 1
                timerListener.onTick(time, 1f - (time.toFloat() / totalTime.toFloat()))
            }
            if (time == 0 && this.isActive || state == TimerState.FINISHED) {
                state = TimerState.FINISHED
                if (stage == PomodoroTimerStage.WORK) {
                    cycles++
                }
                val duration =  when (stage) {
                    PomodoroTimerStage.WORK -> settings.workDuration
                    PomodoroTimerStage.SHORT_BREAK -> settings.shortBreakDuration
                    PomodoroTimerStage.LONG_BREAK -> settings.longBreakDuration
                }
                timerListener.onFinish(stage.mappingStage, changeStage().mappingStage, duration)
                timerListener.onChangeState(state)
                setInitialTime()
            }
        }
    }

    override fun stop() {
        state = TimerState.FINISHED
    }

    override fun reset() {
        timerJob?.cancel()
        state = TimerState.IDLE
        time = setInitialTime()
        timerListener.onChangeState(state)
    }

    override fun pause() {
        if (state == TimerState.IN_PROGRESS) {
            timerJob?.cancel()
            state = TimerState.PAUSE
        } else if (state == TimerState.PAUSE) {
            start()
        }

        timerListener.onPause(state == TimerState.PAUSE)
    }

    override fun getState(): TimerState {
        return state
    }

    override fun getTime(): Int {
        return time
    }

    override fun getStage(): TimerStage {
        return stage.mappingStage
    }

    private fun changeStage(): PomodoroTimerStage {
        stage = when (stage.mappingStage) {
            TimerStage.WORK -> if (cycles % settings.workCycles == 0) {
                PomodoroTimerStage.LONG_BREAK
            } else {
                PomodoroTimerStage.SHORT_BREAK
            }
            TimerStage.REST -> PomodoroTimerStage.WORK
        }
        return stage
    }

    private fun setInitialTime(): Int {
        time = when (stage) {
            PomodoroTimerStage.WORK -> settings.workDuration
            PomodoroTimerStage.LONG_BREAK -> settings.longBreakDuration
            PomodoroTimerStage.SHORT_BREAK -> settings.shortBreakDuration
        }
        totalTime = time
        timerListener.onTick(time, 0f)
        return time
    }

    private enum class PomodoroTimerStage(val mappingStage: TimerStage) {
        WORK(TimerStage.WORK),
        SHORT_BREAK(TimerStage.REST),
        LONG_BREAK(TimerStage.REST),
    }
}
