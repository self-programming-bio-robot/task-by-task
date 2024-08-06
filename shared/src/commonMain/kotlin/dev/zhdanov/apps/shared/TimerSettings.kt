package dev.zhdanov.apps.shared

data class TimerSettings(
    val workDuration: Int,   // in minutes
    val shortBreakDuration: Int,  // in minutes
    val longBreakDuration: Int,   // in minutes
    val workCycles: Int,   // number of work sessions before a long break
)

enum class TimerState {
    WORK,
    BREAK,
    LONG_BREAK
}