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
