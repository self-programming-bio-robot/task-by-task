package dev.zhdanov.apps.shared.model.timer

import kotlinx.coroutines.*

class InfiniteTimer(
    private val coroutineScope: CoroutineScope,
    initialStage: TimerStage,
    override val timerListener: Timer.TimerListener
) : Timer {
    private var stage: TimerStage = initialStage
    private var state: TimerState = TimerState.IDLE
    private var time: Int = setInitialTime()
    private var timerJob: Job? = null

    override fun start() {
        if (state == TimerState.IN_PROGRESS) return
        if (state == TimerState.IDLE) {
            timerListener.onStart(stage)
        }
        state = TimerState.IN_PROGRESS
        timerListener.onChangeState(state)

        timerJob = coroutineScope.launch {
            while (this.isActive && state == TimerState.IN_PROGRESS) {
                delay(1000)
                time += 1
                timerListener.onTick(time, (time % 60) / 60f)
            }
            if (this.isActive && state == TimerState.FINISHED) {
                timerListener.onFinish(stage, changeStage(), time)
                timerListener.onChangeState(state)
                setInitialTime()
            }
        }
    }

    override fun stop() {
        state = TimerState.FINISHED
    }

    override fun reset() {
        stop()
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
        return stage
    }

    private fun changeStage(): TimerStage {
        stage = when (stage) {
            TimerStage.WORK -> TimerStage.REST
            TimerStage.REST -> TimerStage.WORK
        }
        return stage
    }

    private fun setInitialTime(): Int {
        time = 0
        timerListener.onTick(time, 0f)
        return time
    }
}
