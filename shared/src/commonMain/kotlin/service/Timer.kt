package service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import models.TimerBootstrap

interface Timer {
    fun updateTime(time: Instant)
    fun onDone(callback: () -> Unit)
}

class TimerService(
    private val config: TimerBootstrap,
    startTime: Instant = Clock.System.now(),
): Timer {
    private val startTime: Instant = startTime
    private var callback: (() -> Unit)? = null

    override fun updateTime(time: Instant) {
        if (time.minus(startTime) >= config.duration) {
            callback?.let { it() }
        }
    }

    override fun onDone(callback: () -> Unit) {
        this.callback = callback;
    }
}