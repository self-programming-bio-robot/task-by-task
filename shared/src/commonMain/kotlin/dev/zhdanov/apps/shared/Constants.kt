package dev.zhdanov.apps.shared

import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

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

val DEFAULT_START_OF_DAY = LocalTime(5, 0)

/**
 * Serializable representation of start of day time for storing in settings.
 */
@Serializable
data class StartOfDaySetting(
    val hour: Int,
    val minute: Int
) {
    fun toLocalTime(): LocalTime = LocalTime(hour, minute)

    companion object {
        fun fromLocalTime(time: LocalTime): StartOfDaySetting =
            StartOfDaySetting(time.hour, time.minute)

        val DEFAULT: StartOfDaySetting = StartOfDaySetting(5, 0)
    }
}
