package dev.zhdanov.apps.composeApp.services

import com.diamondedge.logging.logging
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.FocusTimeWithTasks
import dev.zhdanov.apps.shared.model.Task
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
    dispatchers: AppDispatchers,
    private val routineDataService: RoutineDataService,
    private val logicalDayService: LogicalDayService
) {
    val finishDayEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private val rolloverMutex = Mutex()
    private val finishAttempts = mutableMapOf<LocalDate, Int>()
    private var lastSyncedRoutineDay: LocalDate? = null

    init {
        updateScheduler()
        startDayRolloverWatchdog()
    }

    fun updateScheduler() {
        coroutineScope.launch {
            val startOfDay = settingsService.getStartOfDay()
            logicalDayService.currentLogicalDay()
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

            routineDataService.syncDailyRoutineTasks(currentDay)
            lastSyncedRoutineDay = currentDay
            settingsService.saveLastDayReset(currentDay)
            finishDayEvents.emit(Unit)
            logger.i { "Processed day rollover: $lastProcessedDay -> $currentDay" }
        } else if (lastSyncedRoutineDay != currentDay) {
            runCatching {
                routineDataService.syncDailyRoutineTasks(currentDay)
                lastSyncedRoutineDay = currentDay
            }.onFailure { logger.e(it) { "Failed to sync daily routine tasks" } }
        }

        if ((finishAttempts[previousDay] ?: 0) < MAX_FINISH_ATTEMPTS) {
            tryFinishDay(previousDay)
        }
    }

    private suspend fun tryFinishDay(day: LocalDate) {
        runCatching {
            if (daySummaryDataService.getDaySummary(day) == null) {
                val focusTimes = getFocusTimesForDay(day)
                val completedTasks = getCompletedTasksForDay(day)
                if (focusTimes.isNotEmpty() || completedTasks.isNotEmpty()) {
                    createDaySummary(day, focusTimes, completedTasks)
                }
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
        return createDaySummary(
            day = dayDate,
            focusTimes = getFocusTimesForDay(dayDate),
            completedTasks = getCompletedTasksForDay(dayDate)
        )
    }

    fun migration() {
        coroutineScope.launch {
            val startOfDayDuration = settingsService.getStartOfDay().toDuration()
            val timeZone = TimeZone.currentSystemDefault()
            val startOfToday = startOfDayWithShift(Clock.System.now(), shift = startOfDayDuration)
            val focusTimesByDay = focusSessionDataService
                .getFocusTimesBetween(0L, startOfToday.toEpochMilliseconds())
                .groupBy {
                    Instant
                        .fromEpochMilliseconds(it.finishedAt)
                        .minus(startOfDayDuration)
                        .toLocalDateTime(timeZone)
                        .date
                }
            val completedTasksByDay = taskDataService
                .getCompletedTasksBetween(0L, startOfToday.toEpochMilliseconds())
                .filter { it.completedAt != null }
                .groupBy {
                    it.completedAt!!
                        .toInstant(timeZone)
                        .minus(startOfDayDuration)
                        .toLocalDateTime(timeZone)
                        .date
                }
            val days = (focusTimesByDay.keys + completedTasksByDay.keys).sorted()
            days.forEach { day ->
                if (daySummaryDataService.getDaySummary(day) == null) {
                    val focusTimes = focusTimesByDay[day].orEmpty()
                    val completedTasks = completedTasksByDay[day].orEmpty()
                    runCatching { createDaySummary(day, focusTimes, completedTasks) }
                        .onFailure { error -> logger.e(error) { "Failed to add day summary" } }
                }
            }
        }
    }

    private suspend fun getDayPeriodBounds(day: LocalDate): Pair<Long, Long> {
        val startOfDay = settingsService.getStartOfDay()
        val timeZone = TimeZone.currentSystemDefault()
        val from = LocalDateTime(day, startOfDay).toInstant(timeZone).toEpochMilliseconds()
        val to = LocalDateTime(day.plus(1, DateTimeUnit.DAY), startOfDay).toInstant(timeZone)
            .toEpochMilliseconds()
        return from to to
    }

    private suspend fun getFocusTimesForDay(day: LocalDate): List<FocusTime> {
        val (from, to) = getDayPeriodBounds(day)
        return focusSessionDataService.getFocusTimesBetween(from = from, to = to)
    }

    private suspend fun getCompletedTasksForDay(day: LocalDate): List<Task> {
        val (from, to) = getDayPeriodBounds(day)
        return taskDataService.getCompletedTasksBetween(from = from, to = to)
    }

    private suspend fun createDaySummary(
        day: LocalDate,
        focusTimes: List<FocusTime>,
        completedTasks: List<Task>
    ): AssistantReviewResponse {
        val allTasksById = taskDataService.getAllTasks().associateBy { it.id }
        val focusTimesWithTasks = resolveFocusTimesWithTasks(focusTimes, allTasksById)
        val review = reviewDay(focusTimesWithTasks, completedTasks)
        daySummaryDataService.addDaySummary(
            DaySummary(
                date = day,
                focusTime = focusTimes.sumOf { it.duration }.toLong(),
                review = review.summary,
                linkedTasks = buildLinkedTasks(focusTimesWithTasks, completedTasks, allTasksById)
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
        logicalDayService.currentLogicalDay(currentDateTime)

    private suspend fun resolveFocusTimesWithTasks(
        focusTimes: List<FocusTime>,
        allTasksById: Map<Long, Task>
    ): List<FocusTimeWithTasks> {
        return focusTimes.map { focusTime ->
            val linkedTasks = focusSessionDataService.getTasksForFocusTime(focusTime.id)
            val resolvedTasks = linkedTasks.ifEmpty {
                focusTime.taskId?.let { taskId -> listOfNotNull(allTasksById[taskId]) }.orEmpty()
            }
            FocusTimeWithTasks(
                focusTime = focusTime,
                tasks = resolvedTasks
            )
        }
    }

    private fun buildLinkedTasks(
        focusTimesWithTasks: List<FocusTimeWithTasks>,
        completedTasks: List<Task>,
        allTasksById: Map<Long, Task>
    ): List<TaskSummary> {
        val knownTaskTitles = allTasksById
            .mapValues { (_, task) -> task.title }
            .toMutableMap()
        val durationByTask = linkedMapOf<Long, Long>()

        focusTimesWithTasks.forEach { item ->
            val focusTime = item.focusTime
            val taskIds = if (item.tasks.isNotEmpty()) {
                item.tasks.map { task ->
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

        completedTasks.forEach { task ->
            knownTaskTitles[task.id] = task.title
            if (task.id !in durationByTask) {
                durationByTask[task.id] = 0L
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

    private suspend fun reviewDay(
        focusTimesWithTasks: List<FocusTimeWithTasks>,
        completedTasks: List<Task>
    ): DayReviewResult {
        val assistantConfig = settingsService.getAssistantConfig()
            ?: throw MissingOpenAiTokenException()

        val focusSection = if (focusTimesWithTasks.isEmpty()) {
            "No focus sessions recorded."
        } else {
            focusTimesWithTasks.joinToString(separator = "\n\n") { item ->
                val ft = item.focusTime
                val tasksLine = if (item.tasks.isNotEmpty()) {
                    "Tasks: ${item.tasks.joinToString(", ") { it.title }}"
                } else {
                    null
                }
                buildString {
                    appendLine("Date: ${ft.finishedAt.toLocalDateTime()}")
                    appendLine("Duration: ${ft.duration.seconds}")
                    if (tasksLine != null) {
                        appendLine(tasksLine)
                    }
                    if (ft.feedback.isNotBlank()) {
                        append(ft.feedback)
                    }
                }.trimEnd()
            }
        }

        val completedTasksSection = if (completedTasks.isEmpty()) {
            "No tasks completed."
        } else {
            completedTasks.joinToString(separator = "\n") { task ->
                val desc = task.description?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                "- ${task.title}$desc"
            }
        }

        val historyOfDay = """
            |Focus sessions:
            |$focusSection
            |
            |Completed tasks:
            |$completedTasksSection
        """.trimMargin()

        return reviewClient.reviewDay(assistantConfig, historyOfDay)
    }

    companion object {
        private val logger = logging()
        private val ROLLOVER_CHECK_INTERVAL = 1.minutes
        private const val MAX_FINISH_ATTEMPTS = 120
    }
}

class MissingOpenAiTokenException : IllegalStateException("OpenAI token not found")
