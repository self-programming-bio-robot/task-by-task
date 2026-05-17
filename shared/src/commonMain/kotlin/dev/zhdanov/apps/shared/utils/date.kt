@file:OptIn(ExperimentalTime::class)

package dev.zhdanov.apps.shared.utils

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun LocalDate.toLong() =
    (this.year * 10000 + (this.month.ordinal + 1) * 100 + this.day).toLong()

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

