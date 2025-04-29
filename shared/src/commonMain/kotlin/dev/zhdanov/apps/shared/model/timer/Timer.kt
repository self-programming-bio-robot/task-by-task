package dev.zhdanov.apps.shared.model.timer

interface Timer {

    interface TimerListener {
        fun onStart(stage: TimerStage)
        fun onFinish(old: TimerStage, new: TimerStage, duration: Int)
        fun onPause(isPaused: Boolean)
        fun onTick(time: Int, progress: Float)
        fun onChangeState(state: TimerState)
    }

    val timerListener: TimerListener

    fun start()

    fun stop()

    fun reset()

    fun pause()

    fun getState(): TimerState

    fun getTime(): Int

    fun getStage(): TimerStage
}



enum class TimerState {
    IDLE,
    IN_PROGRESS,
    PAUSE,
    FINISHED,
}

enum class TimerStage {
    WORK,
    REST
}
