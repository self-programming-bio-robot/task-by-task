package dev.zhdanov.apps.shared.model

data class TimerSettings(
    val id: Long = -1,
    val default: Boolean,
    val workDuration: Int,   // in seconds
    val shortBreakDuration: Int,  // in seconds
    val longBreakDuration: Int,   // in seconds
    val workCycles: Int,   // number of work sessions before a long break
)

enum class TimerState {
    WORK,
    BREAK,
    LONG_BREAK,
    FEEDBACK
}
