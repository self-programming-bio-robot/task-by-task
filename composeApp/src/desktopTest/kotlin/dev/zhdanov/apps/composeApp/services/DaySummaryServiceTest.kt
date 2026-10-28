package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.model.AssistantConfig
import dev.zhdanov.apps.shared.model.TaskSummary
import dev.zhdanov.apps.shared.utils.toDuration
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class DaySummaryServiceTest {
    @Test
    fun `finishDay uses configured start of day and summarizes junction linked tasks`() = runTest {
        val fixture = createFixture()
        fixture.settingsService.saveAssistantConfig(
            "token",
            "https://api.openai.com/v1/",
            "gpt-4.1"
        )
        fixture.settingsService.saveStartOfDay(LocalTime(5, 0))
        fixture.taskDataService.addTask("Refactor")
        val task = fixture.taskDataService.getAllTasks().first()
        val timeZone = TimeZone.currentSystemDefault()

        fixture.focusSessionDataService.addFocusTimeWithTasks(
            duration = 1_500,
            finishedAt = LocalDateTime(2026, 5, 15, 6, 0).toInstant(timeZone).toEpochMilliseconds(),
            feedback = "Deep work",
            startedAt = null,
            pauseTime = null,
            taskIds = listOf(task.id)
        )

        val response = fixture.daySummaryService.finishDay(
            LocalDateTime(2026, 5, 16, 4, 59, 59).toInstant(timeZone)
        )
        val savedSummary = fixture.daySummaryDataService.getDaySummary(LocalDate(2026, 5, 15))

        assertEquals(LocalDate(2026, 5, 15), response.date)
        assertEquals("summary", response.summary)
        assertEquals(listOf(TaskSummary(task.id, "Refactor", 1_500)), savedSummary?.linkedTasks)
        assertTrue(fixture.reviewClient.lastHistoryOfDay.orEmpty().contains("Tasks: Refactor"))
    }

    @Test
    fun `finishDay links completed tasks without focus sessions and includes them in llm prompt`() = runTest {
        val fixture = createFixture()
        fixture.settingsService.saveAssistantConfig(
            "token",
            "https://api.openai.com/v1/",
            "gpt-4.1"
        )
        val startOfDay = LocalTime(5, 0)
        fixture.settingsService.saveStartOfDay(startOfDay)
        val timeZone = TimeZone.currentSystemDefault()

        fixture.taskDataService.addTask("Focused task")
        fixture.taskDataService.addTask("Completed without focus")
        val tasks = fixture.taskDataService.getAllTasks()
        val focusedTask = tasks.first { it.title == "Focused task" }
        val completedTask = tasks.first { it.title == "Completed without focus" }

        fixture.taskDataService.completeTask(completedTask.id)
        val now = Clock.System.now()
        val currentDay = now.minus(startOfDay.toDuration()).toLocalDateTime(timeZone).date

        fixture.focusSessionDataService.addFocusTimeWithTasks(
            duration = 1_200,
            finishedAt = now.toEpochMilliseconds(),
            feedback = "Worked on feature",
            startedAt = null,
            pauseTime = null,
            taskIds = listOf(focusedTask.id)
        )

        fixture.daySummaryService.finishDay(now)
        val savedSummary = fixture.daySummaryDataService.getDaySummary(currentDay)

        assertEquals(
            listOf(
                TaskSummary(focusedTask.id, "Focused task", 1_200),
                TaskSummary(completedTask.id, "Completed without focus", 0)
            ),
            savedSummary?.linkedTasks
        )
        val promptHistory = fixture.reviewClient.lastHistoryOfDay.orEmpty()
        assertTrue(promptHistory.contains("Tasks: Focused task"))
        assertTrue(promptHistory.contains("- Completed without focus"))
    }

    @Test
    fun `finishDay summarizes day when only completed tasks exist`() = runTest {
        val fixture = createFixture()
        fixture.settingsService.saveAssistantConfig(
            "token",
            "https://api.openai.com/v1/",
            "gpt-4.1"
        )
        val startOfDay = LocalTime(5, 0)
        fixture.settingsService.saveStartOfDay(startOfDay)

        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val currentDay = now
            .minus(startOfDay.toDuration())
            .toLocalDateTime(timeZone)
            .date

        fixture.taskDataService.addTask("Quick chore")
        val task = fixture.taskDataService.getAllTasks().first()
        fixture.taskDataService.completeTask(task.id)

        val response = fixture.daySummaryService.finishDay(now)
        val savedSummary = fixture.daySummaryDataService.getDaySummary(currentDay)

        assertEquals(currentDay, response.date)
        assertNotNull(savedSummary)
        assertEquals(0L, savedSummary.focusTime)
        assertEquals(listOf(TaskSummary(task.id, "Quick chore", 0)), savedSummary.linkedTasks)
        assertTrue(fixture.reviewClient.lastHistoryOfDay.orEmpty().contains("- Quick chore"))
    }

    @Test
    fun `missing OpenAI token leaves current day active`() = runTest {
        val fixture = createFixture()
        val timeZone = TimeZone.currentSystemDefault()
        val finishTime = LocalDateTime(2026, 5, 16, 4, 59, 59).toInstant(timeZone)

        assertFailsWith<MissingOpenAiTokenException> {
            fixture.daySummaryService.finishDay(finishTime)
        }

        assertTrue(fixture.daySummaryService.isCurrentDayActive(finishTime))
    }

    @Test
    fun `scheduled rollover finishes missed previous day and resets today tasks`() = runTest {
        val fixture = createFixture()
        fixture.settingsService.saveAssistantConfig(
            "token",
            "https://api.openai.com/v1/",
            "gpt-4.1"
        )
        val startOfDay = LocalTime(5, 0)
        fixture.settingsService.saveStartOfDay(startOfDay)

        val timeZone = TimeZone.currentSystemDefault()
        val currentDay = Clock.System.now()
            .minus(startOfDay.toDuration())
            .toLocalDateTime(timeZone)
            .date
        val previousDay = currentDay.minus(1, DateTimeUnit.DAY)
        fixture.settingsService.saveLastDayReset(previousDay)

        fixture.taskDataService.addTask("Stale today task", targetDate = previousDay)
        val task = fixture.taskDataService.getAllTasks().first()
        fixture.focusSessionDataService.addFocusTimeWithTasks(
            duration = 600,
            finishedAt = LocalDateTime(previousDay, LocalTime(12, 0))
                .toInstant(timeZone).toEpochMilliseconds(),
            feedback = "work",
            startedAt = null,
            pauseTime = null,
            taskIds = listOf(task.id)
        )

        fixture.schedulerService.scheduledActions.single().invoke(
            Clock.System.now(),
            Clock.System.now(),
            timeZone
        )

        assertNotNull(fixture.daySummaryDataService.getDaySummary(previousDay))
        assertTrue(fixture.taskDataService.getAllTasks().none { it.targetDate == currentDay.toLong() })
        assertTrue(fixture.daySummaryService.isCurrentDayActive())
    }

    @Test
    fun `scheduled rollover resets today tasks when day was finished manually`() = runTest {
        val fixture = createFixture()
        fixture.settingsService.saveAssistantConfig(
            "token",
            "https://api.openai.com/v1/",
            "gpt-4.1"
        )
        val startOfDay = LocalTime(5, 0)
        fixture.settingsService.saveStartOfDay(startOfDay)

        val timeZone = TimeZone.currentSystemDefault()
        val currentDay = Clock.System.now()
            .minus(startOfDay.toDuration())
            .toLocalDateTime(timeZone)
            .date
        val previousDay = currentDay.minus(1, DateTimeUnit.DAY)
        fixture.settingsService.saveLastDayReset(previousDay)

        fixture.taskDataService.addTask("Stale today task", targetDate = previousDay)
        fixture.daySummaryService.finishDay(
            LocalDateTime(previousDay, LocalTime(20, 0)).toInstant(timeZone)
        )

        fixture.schedulerService.scheduledActions.single().invoke(
            Clock.System.now(),
            Clock.System.now(),
            timeZone
        )

        assertTrue(fixture.taskDataService.getAllTasks().none { it.targetDate == currentDay.toLong() })
        assertTrue(fixture.daySummaryService.isCurrentDayActive())
    }

    private fun createFixture(): Fixture {
        val database = Database(InMemoryDriverFactory())
        val workspaceSessionService = createWorkspaceSessionService(database)
        val dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher()
        )
        val settingsService = AppSettingsService(database, dispatchers, workspaceSessionService)
        val logicalDayService = LogicalDayService(settingsService)
        val taskDataService = TaskDataService(database, dispatchers, workspaceSessionService)
        val routineDataService =
            RoutineDataService(database, dispatchers, workspaceSessionService, logicalDayService)
        val focusSessionDataService =
            FocusSessionDataService(database, dispatchers, workspaceSessionService)
        val daySummaryDataService =
            DaySummaryDataService(database, dispatchers, workspaceSessionService)
        val schedulerService = RecordingSchedulerService()
        val reviewClient = FakeReviewClient()
        val daySummaryService = DaySummaryService(
            daySummaryDataService = daySummaryDataService,
            focusSessionDataService = focusSessionDataService,
            taskDataService = taskDataService,
            settingsService = settingsService,
            schedulerService = schedulerService,
            reviewClient = reviewClient,
            dispatchers = dispatchers,
            routineDataService = routineDataService,
            logicalDayService = logicalDayService
        )

        return Fixture(
            settingsService = settingsService,
            taskDataService = taskDataService,
            routineDataService = routineDataService,
            focusSessionDataService = focusSessionDataService,
            daySummaryDataService = daySummaryDataService,
            daySummaryService = daySummaryService,
            schedulerService = schedulerService,
            reviewClient = reviewClient
        )
    }
}

private data class Fixture(
    val settingsService: AppSettingsService,
    val taskDataService: TaskDataService,
    val routineDataService: RoutineDataService,
    val focusSessionDataService: FocusSessionDataService,
    val daySummaryDataService: DaySummaryDataService,
    val daySummaryService: DaySummaryService,
    val schedulerService: RecordingSchedulerService,
    val reviewClient: FakeReviewClient
)

private class FakeReviewClient : ReviewClient {
    var lastHistoryOfDay: String? = null

    override suspend fun reviewDay(config: AssistantConfig, historyOfDay: String): DayReviewResult {
        lastHistoryOfDay = historyOfDay
        return DayReviewResult(summary = "summary", response = "response")
    }
}

private class RecordingSchedulerService : SchedulerService {
    val scheduledActions = mutableListOf<SchedulerAction>()

    override fun addScheduler(
        tag: String,
        cron: String,
        timeZone: TimeZone,
        action: SchedulerAction
    ) {
        scheduledActions.add(action)
    }

    override fun addScheduler(tag: String, cron: String, action: SchedulerAction) = Unit
}

private class InMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}
