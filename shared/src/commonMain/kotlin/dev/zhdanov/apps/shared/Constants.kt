package dev.zhdanov.apps.shared

import dev.zhdanov.apps.shared.model.TimerSettings

const val SERVER_PORT = 8080

val DEFAULT_TIMER_SETTINGS = TimerSettings(
    id = -100,
    default = true,
    workDuration = 25 * 60,
    shortBreakDuration = 5 * 60,
    longBreakDuration = 15 * 60,
    workCycles = 4,
)

val INFINITE_TIMER_SETTINGS = TimerSettings(
    id = -101,
    default = false,
    workDuration = -1,
    shortBreakDuration = -1,
    longBreakDuration = -1,
    workCycles = 1,
    isInfinite = true,
)
