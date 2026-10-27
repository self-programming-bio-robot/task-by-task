package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.utils.toDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _currentDay = MutableStateFlow(
        Clock.System.now()
            .minus(settingsService.startOfDay.value.toDuration())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    )
    val currentDay: StateFlow<LocalDate> = _currentDay.asStateFlow()

    suspend fun currentLogicalDay(
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDate {
        return computeLogicalDay(now, timeZone).also {
            _currentDay.value = it
        }
    }

    suspend fun logicalDayFor(
        dateTime: LocalDateTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): LocalDate {
        return computeLogicalDay(dateTime.toInstant(timeZone), timeZone)
    }

    private suspend fun computeLogicalDay(
        instant: Instant,
        timeZone: TimeZone
    ): LocalDate {
        val shift = settingsService.getStartOfDay().toDuration()
        return instant.minus(shift).toLocalDateTime(timeZone).date
    }
}
