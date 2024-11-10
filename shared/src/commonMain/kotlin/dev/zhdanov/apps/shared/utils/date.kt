package dev.zhdanov.apps.shared.utils

import kotlinx.datetime.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun LocalDate.toLong() =
    (this.year * 10000 + this.monthNumber * 100 + this.dayOfMonth).toLong()

fun Long.toLocalDate(): LocalDate {
    val year = this / 10000
    val month = (this % 10000) / 100
    val day = this % 100

    return LocalDate(year.toInt(), month.toInt(), day.toInt())
}

fun Long.toLocalDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

fun LocalTime.toDuration() = this.toSecondOfDay().seconds

fun startOfDayWithShift(
    time: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    shift: Duration = Duration.ZERO
): Instant {
    return time
        .minus(shift)
        .toLocalDateTime(timeZone)
        .date
        .atStartOfDayIn(timeZone)
        .plus(shift)
}

