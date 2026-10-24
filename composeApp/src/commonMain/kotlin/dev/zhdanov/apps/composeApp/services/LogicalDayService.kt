package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.utils.toDuration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LogicalDayService(
    private val settingsService: AppSettingsService
) {
    suspend fun currentLogicalDay(
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDate {
        val shift = settingsService.getStartOfDay().toDuration()
        return now.minus(shift).toLocalDateTime(timeZone).date
    }

    suspend fun logicalDayFor(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDate {
        return currentLogicalDay(dateTime.toInstant(timeZone), timeZone)
    }
}
