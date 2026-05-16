package dev.zhdanov.apps.composeApp.services

import com.diamondedge.logging.logging
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.TaskSummary
import dev.zhdanov.apps.shared.utils.startOfDayWithShift
import dev.zhdanov.apps.shared.utils.toDuration
import dev.zhdanov.apps.shared.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class DaySummaryService(
    private val daySummaryDataService: DaySummaryDataService,
    private val focusSessionDataService: FocusSessionDataService,
    private val taskDataService: TaskDataService,
    private val settingsService: AppSettingsService,
    private val schedulerService: SchedulerService,
    private val reviewClient: ReviewClient,
    dispatchers: AppDispatchers
) {
    val finishDayEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    init {
        updateScheduler()
    }

    fun updateScheduler() {
        coroutineScope.launch {
            val startOfDay = settingsService.getStartOfDay()
            schedulerService.addScheduler(
                "Finish day",
                "${startOfDay.minute} ${startOfDay.hour} * * *",
                TimeZone.currentSystemDefault()
            ) { plannedTime, actualTime, timeZone ->
                coroutineScope.launch {
                    runCatching {
                        finishDay(plannedTime.minus(1.seconds))
                        taskDataService.cleanTodayTaskList()
                        finishDayEvents.emit(Unit)
                        logger.i {
                            "Finish day at ${actualTime.toLocalDateTime(timeZone)} for planned time: ${plannedTime.toLocalDateTime(timeZone)}"
                        }
                    }.onFailure { error ->
                        logger.i {
                            "Skip finishing day at ${actualTime.toLocalDateTime(timeZone)} for planned time: ${plannedTime.toLocalDateTime(timeZone)}: ${error.message}"
                        }
                    }
                }
            }
        }
    }

    suspend fun isCurrentDayActive(currentDateTime: Instant = Clock.System.now()): Boolean {
        return daySummaryDataService.getDaySummary(dayDateFor(currentDateTime)) == null
    }

    suspend fun finishDay(currentDateTime: Instant = Clock.System.now()): AssistantReviewResponse {
        val dayDate = dayDateFor(currentDateTime)

        if (daySummaryDataService.getDaySummary(dayDate) != null) {
            logger.i { "Day summary already exists for $dayDate" }
            throw IllegalStateException("Day summary already exists for $dayDate")
        }

        val startOfDay = settingsService.getStartOfDay()
        val startDateTime = LocalDateTime(dayDate, startOfDay)
        val endDateTime = LocalDateTime(dayDate.plus(1, DateTimeUnit.DAY), startOfDay)
        val timeZone = TimeZone.currentSystemDefault()

        val focusTimes = focusSessionDataService.getFocusTimesBetween(
            from = startDateTime.toInstant(timeZone).toEpochMilliseconds(),
            to = endDateTime.toInstant(timeZone).toEpochMilliseconds()
        )

        val linkedTasks = buildLinkedTasks(focusTimes)
        val review = reviewDay(focusTimes)

        daySummaryDataService.addDaySummary(
            DaySummary(
                date = dayDate,
                focusTime = focusTimes.sumOf { it.duration }.toLong(),
                review = review.summary,
                linkedTasks = linkedTasks
            )
        )

        logger.d { review }
        return AssistantReviewResponse(
            date = dayDate,
            summary = review.summary,
            response = review.response
        )
    }

    fun migration() {
        coroutineScope.launch {
            val startOfDayDuration = settingsService.getStartOfDay().toDuration()
            val startOfToday = startOfDayWithShift(Clock.System.now(), shift = startOfDayDuration)
            focusSessionDataService.getFocusTimesBetween(0L, startOfToday.toEpochMilliseconds())
                .groupBy {
                    Instant
                        .fromEpochMilliseconds(it.finishedAt)
                        .minus(startOfDayDuration)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .date
                }
                .forEach { group ->
                    if (daySummaryDataService.getDaySummary(group.key) == null) {
                        runCatching {
                            val review = reviewDay(group.value)
                            daySummaryDataService.addDaySummary(
                                DaySummary(
                                    date = group.key,
                                    focusTime = group.value.sumOf { it.duration }.toLong(),
                                    review = review.summary,
                                    linkedTasks = buildLinkedTasks(group.value)
                                )
                            )
                        }.onFailure { error ->
                            logger.e(error) { "Failed to add day summary" }
                        }
                    }
                }
        }
    }

    private suspend fun dayDateFor(currentDateTime: Instant) =
        currentDateTime
            .minus(settingsService.getStartOfDay().toDuration())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    private suspend fun buildLinkedTasks(focusTimes: List<FocusTime>): List<TaskSummary> {
        val knownTaskTitles = taskDataService.getAllTasks()
            .associate { it.id to it.title }
            .toMutableMap()
        val durationByTask = mutableMapOf<Long, Long>()

        focusTimes.forEach { focusTime ->
            val linkedTasks = focusSessionDataService.getTasksForFocusTime(focusTime.id)
            val taskIds = if (linkedTasks.isNotEmpty()) {
                linkedTasks.map { task ->
                    knownTaskTitles[task.id] = task.title
                    task.id
                }
            } else {
                listOfNotNull(focusTime.taskId)
            }

            taskIds.forEach { taskId ->
                durationByTask[taskId] = (durationByTask[taskId] ?: 0L) + focusTime.duration
            }
        }

        return durationByTask
            .map { (taskId, duration) ->
                TaskSummary(
                    taskId = taskId,
                    title = knownTaskTitles[taskId] ?: "Unknown task",
                    totalDuration = duration
                )
            }
            .sortedByDescending { it.totalDuration }
    }

    private suspend fun reviewDay(focusTimes: List<FocusTime>): DayReviewResult {
        val assistantConfig = settingsService.getAssistantConfig()
            ?: throw MissingOpenAiTokenException()

        val historyOfDay = focusTimes.joinToString(separator = "\n") {
            """Date: ${it.finishedAt.toLocalDateTime()}
                |Duration: ${it.duration.seconds}
                |${it.feedback}
            """.trimMargin()
        }

        return reviewClient.reviewDay(assistantConfig, historyOfDay)
    }

    companion object {
        private val logger = logging()
    }
}

class MissingOpenAiTokenException : IllegalStateException("OpenAI token not found")
