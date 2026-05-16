package dev.zhdanov.apps.shared.model.timer

import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TimerTest {
    @Test
    fun `pomodoro timer finishes work stage deterministically`() = runTest {
        val listener = RecordingTimerListener()
        val timer = PomodoroTimer(
            settings = TimerSettings(
                id = 1,
                default = false,
                workDuration = 2,
                shortBreakDuration = 1,
                longBreakDuration = 1,
                workCycles = 2
            ),
            coroutineScope = this,
            initialStage = TimerStage.WORK,
            timerListener = listener,
            ticker = TimerTicker { }
        )

        timer.start()
        runCurrent()

        assertEquals(TimerState.FINISHED, timer.getState())
        assertEquals(listOf(2, 1, 0, 1), listener.ticks)
        assertEquals(listOf(2), listener.finishedDurations)
    }

    @Test
    fun `infinite timer reset cancels running timer and clears time`() = runTest {
        val listener = RecordingTimerListener()
        val timer = InfiniteTimer(
            coroutineScope = this,
            initialStage = TimerStage.WORK,
            timerListener = listener,
            ticker = OneTickThenSuspendTicker()
        )

        timer.start()
        runCurrent()
        timer.reset()

        assertEquals(TimerState.IDLE, timer.getState())
        assertEquals(0, timer.getTime())
        assertEquals(0, listener.ticks.last())
    }
}

private class OneTickThenSuspendTicker : TimerTicker {
    private var tickCount = 0

    override suspend fun waitForNextTick() {
        if (tickCount++ == 0) {
            yield()
        } else {
            awaitCancellation()
        }
    }
}

private class RecordingTimerListener : Timer.TimerListener {
    val ticks = mutableListOf<Int>()
    val finishedDurations = mutableListOf<Int>()

    override fun onStart(stage: TimerStage) = Unit

    override fun onFinish(old: TimerStage, new: TimerStage, duration: Int) {
        finishedDurations.add(duration)
    }

    override fun onPause(isPaused: Boolean) = Unit

    override fun onTick(time: Int, progress: Float) {
        ticks.add(time)
    }

    override fun onChangeState(state: TimerState) = Unit
}
