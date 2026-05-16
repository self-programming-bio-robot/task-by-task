package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.model.TaskSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class DaySummaryServiceTest {
    @Test
    fun `finishDay uses configured start of day and summarizes junction linked tasks`() = runTest {
        val fixture = createFixture()
        fixture.settingsService.saveOpenAiToken("token")
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

    private fun createFixture(): Fixture {
        val database = Database(InMemoryDriverFactory())
        val dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher()
        )
        val settingsService = AppSettingsService(database, dispatchers)
        val taskDataService = TaskDataService(database, dispatchers)
        val focusSessionDataService = FocusSessionDataService(database, dispatchers)
        val daySummaryDataService = DaySummaryDataService(database, dispatchers)
        val schedulerService = RecordingSchedulerService()
        val daySummaryService = DaySummaryService(
            daySummaryDataService = daySummaryDataService,
            focusSessionDataService = focusSessionDataService,
            taskDataService = taskDataService,
            settingsService = settingsService,
            schedulerService = schedulerService,
            reviewClient = FakeReviewClient(),
            dispatchers = dispatchers
        )

        return Fixture(
            settingsService = settingsService,
            taskDataService = taskDataService,
            focusSessionDataService = focusSessionDataService,
            daySummaryDataService = daySummaryDataService,
            daySummaryService = daySummaryService
        )
    }
}

private data class Fixture(
    val settingsService: AppSettingsService,
    val taskDataService: TaskDataService,
    val focusSessionDataService: FocusSessionDataService,
    val daySummaryDataService: DaySummaryDataService,
    val daySummaryService: DaySummaryService
)

private class FakeReviewClient : ReviewClient {
    override suspend fun reviewDay(token: String, historyOfDay: String): DayReviewResult {
        return DayReviewResult(summary = "summary", response = "response")
    }
}

private class RecordingSchedulerService : SchedulerService {
    override fun addScheduler(tag: String, cron: String, timeZone: TimeZone, action: SchedulerAction) = Unit

    override fun addScheduler(tag: String, cron: String, action: SchedulerAction) = Unit
}

private class InMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}
