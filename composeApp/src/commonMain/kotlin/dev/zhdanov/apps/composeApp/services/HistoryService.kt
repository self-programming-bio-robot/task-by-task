package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTimeWithTasks
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

class HistoryService(
    private val daySummaryDataService: DaySummaryDataService,
    private val focusSessionDataService: FocusSessionDataService,
    private val taskDataService: TaskDataService,
    private val settingsService: AppSettingsService
) {
    suspend fun getHistory(): List<DaySummary> {
        return daySummaryDataService.getAllDaySummaries()
    }

    suspend fun getDaySummary(date: LocalDate): DaySummary? {
        return daySummaryDataService.getDaySummary(date)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getFocusTimesWithTasksForDate(date: LocalDate): List<FocusTimeWithTasks> {
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = settingsService.getStartOfDay()
        val startInstant = LocalDateTime(date, startOfDay).toInstant(timeZone)
        val endInstant = LocalDateTime(date.plus(1, DateTimeUnit.DAY), startOfDay).toInstant(timeZone)

        val tasksById = taskDataService.getAllTasks().associateBy { it.id }

        return focusSessionDataService
            .getFocusTimesBetween(startInstant.toEpochMilliseconds(), endInstant.toEpochMilliseconds())
            .map { focusTime ->
                val linkedTasks = focusSessionDataService.getTasksForFocusTime(focusTime.id)
                FocusTimeWithTasks(
                    focusTime = focusTime,
                    tasks = linkedTasks.ifEmpty {
                        focusTime.taskId?.let { taskId -> listOfNotNull(tasksById[taskId]) }.orEmpty()
                    }
                )
            }
    }
}
