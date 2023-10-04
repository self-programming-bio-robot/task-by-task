package models

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class TimerSettings(
    val baseDuration: Duration = 25.toDuration(DurationUnit.SECONDS),
    val shortRestDuration: Duration = 5.toDuration(DurationUnit.MINUTES),
    val longRestDuration: Duration = 15.toDuration(DurationUnit.MINUTES),
    val sequenceWorkingLength: Int = 4,
    val autoStartRest: Boolean = true,
    val autoStartWork: Boolean = false,
)

data class TimerBootstrap(
    val duration: Duration,
)