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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
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

    private val rolloverMutex = Mutex()
    private val finishAttempts = mutableMapOf<LocalDate, Int>()

    init {
        updateScheduler()
        startDayRolloverWatchdog()
    }

    fun updateScheduler() {
        coroutineScope.launch {
            val startOfDay = settingsService.getStartOfDay()
            schedulerService.addScheduler(
                "Finish day",
                "${startOfDay.minute} ${startOfDay.hour} * * *",
                TimeZone.currentSystemDefault()
            ) { _, _, _ ->
                coroutineScope.launch { runDayRollover() }
            }
        }
    }

    private fun startDayRolloverWatchdog() {
        coroutineScope.launch {
            while (true) {
                runDayRollover()
                delay(ROLLOVER_CHECK_INTERVAL)
            }
        }
    }

    private suspend fun runDayRollover() {
        runCatching { processDayRollover() }
            .onFailure { logger.e(it) { "Day rollover failed" } }
    }

    private suspend fun processDayRollover() = rolloverMutex.withLock {
        val currentDay = dayDateFor(Clock.System.now())
        val previousDay = currentDay.minus(1, DateTimeUnit.DAY)
        val lastProcessedDay = settingsService.getLastDayReset()
            ?: currentDay.also { settingsService.saveLastDayReset(it) }

        if (lastProcessedDay < currentDay) {
            generateSequence(lastProcessedDay) { it.plus(1, DateTimeUnit.DAY) }
                .takeWhile { it < previousDay }
                .forEach { tryFinishDay(it) }

            taskDataService.cleanTodayTaskList()
            settingsService.saveLastDayReset(currentDay)
            finishDayEvents.emit(Unit)
            logger.i { "Processed day rollover: $lastProcessedDay -> $currentDay" }
        }

        if ((finishAttempts[previousDay] ?: 0) < MAX_FINISH_ATTEMPTS) {
            tryFinishDay(previousDay)
        }
    }

    private suspend fun tryFinishDay(day: LocalDate) {
        runCatching {
            if (daySummaryDataService.getDaySummary(day) == null) {
                getFocusTimesForDay(day)
                    .takeIf { it.isNotEmpty() }
                    ?.let { createDaySummary(day, it) }
            }
        }.onSuccess {
            finishAttempts.remove(day)
        }.onFailure { error ->
            finishAttempts[day] = (finishAttempts[day] ?: 0) + 1
            logger.i { "Skip finishing day $day: ${error.message}" }
        }
    }

    suspend fun isCurrentDayActive(currentDateTime: Instant = Clock.System.now()): Boolean {
        return daySummaryDataService.getDaySummary(dayDateFor(currentDateTime)) == null
    }

    suspend fun finishDay(currentDateTime: Instant = Clock.System.now()): AssistantReviewResponse {
        val dayDate = dayDateFor(currentDateTime)
        check(daySummaryDataService.getDaySummary(dayDate) == null) {
            "Day summary already exists for $dayDate"
        }
        return createDaySummary(dayDate, getFocusTimesForDay(dayDate))
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
                .forEach { (day, focusTimes) ->
                    if (daySummaryDataService.getDaySummary(day) == null) {
                        runCatching { createDaySummary(day, focusTimes) }
                            .onFailure { error -> logger.e(error) { "Failed to add day summary" } }
                    }
                }
        }
    }

    private suspend fun getFocusTimesForDay(day: LocalDate): List<FocusTime> {
        val startOfDay = settingsService.getStartOfDay()
        val timeZone = TimeZone.currentSystemDefault()
        return focusSessionDataService.getFocusTimesBetween(
            from = LocalDateTime(day, startOfDay).toInstant(timeZone).toEpochMilliseconds(),
            to = LocalDateTime(day.plus(1, DateTimeUnit.DAY), startOfDay).toInstant(timeZone)
                .toEpochMilliseconds()
        )
    }

    private suspend fun createDaySummary(
        day: LocalDate,
        focusTimes: List<FocusTime>
    ): AssistantReviewResponse {
        val review = reviewDay(focusTimes)
        daySummaryDataService.addDaySummary(
            DaySummary(
                date = day,
                focusTime = focusTimes.sumOf { it.duration }.toLong(),
                review = review.summary,
                linkedTasks = buildLinkedTasks(focusTimes)
            )
        )
        logger.d { review }
        return AssistantReviewResponse(
            date = day,
            summary = review.summary,
            response = review.response
        )
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
        private val ROLLOVER_CHECK_INTERVAL = 1.minutes
        private const val MAX_FINISH_ATTEMPTS = 120
    }
}

class MissingOpenAiTokenException : IllegalStateException("OpenAI token not found")
