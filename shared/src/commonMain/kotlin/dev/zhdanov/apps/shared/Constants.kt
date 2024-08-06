package dev.zhdanov.apps.shared

const val SERVER_PORT = 8080

val DEFAULT_TIMER_SETTINGS = TimerSettings(
    workDuration = 25 * 60,
    shortBreakDuration = 5 * 60,
    longBreakDuration = 15 * 60,
    workCycles = 4,
)

val TEST_TIMER_SETTINGS = TimerSettings(
    workDuration = 15,
    shortBreakDuration = 5,
    longBreakDuration = 10,
    workCycles = 2,
)