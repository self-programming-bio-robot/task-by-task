package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.composeApp.components.timer.TimerViewState
import dev.zhdanov.apps.composeApp.notification.FakeNotificationService
import dev.zhdanov.apps.shared.INFINITE_TIMER_SETTINGS
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dev.zhdanov.apps.shared.model.CreateFocusTime
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TimerSessionServiceTest {
    @Test
    fun `stopping infinite work timer opens feedback for elapsed duration`() = runTest {
        val fixture = createFixture(testScheduler)
        val service = fixture.timerSessionService

        service.changeTimerSettings(INFINITE_TIMER_SETTINGS)
        service.startTimer()
        advanceTimeBy(2_000)
        runCurrent()
        service.stopTimer()

        assertEquals(TimerViewState.FEEDBACK, service.state.value)
        assertEquals(2, service.lastPartDuration.value)
        assertFalse(service.isRunning.value)
    }

    @Test
    fun `feedback from notification is saved, closes feedback and starts rest timer`() = runTest {
        val fixture = createFixture(testScheduler)
        val service = fixture.timerSessionService
        finishInfiniteWork(service)

        fixture.notifications.respond(0, mapOf("feedback" to "Went well"))
        runCurrent()

        assertEquals(TimerViewState.BREAK, service.state.value)
        assertTrue(service.isRunning.value)
        assertEquals(listOf("Went well"), fixture.focusSessionDataService.getAllFocusTimes().map { it.feedback })
        service.stopTimer()
    }

    @Test
    fun `feedback from notification is ignored after feedback screen was closed`() = runTest {
        val fixture = createFixture(testScheduler)
        val service = fixture.timerSessionService
        finishInfiniteWork(service)

        service.submitFeedback(null)
        fixture.notifications.respond(0, mapOf("feedback" to "Late answer"))
        runCurrent()

        assertEquals(TimerViewState.BREAK, service.state.value)
        assertFalse(service.isRunning.value)
        assertTrue(fixture.focusSessionDataService.getAllFocusTimes().isEmpty())
    }

    @Test
    fun `dismissed feedback notification keeps feedback screen open`() = runTest {
        val fixture = createFixture(testScheduler)
        val service = fixture.timerSessionService
        finishInfiniteWork(service)

        fixture.notifications.respond(0, null)
        runCurrent()

        assertEquals(TimerViewState.FEEDBACK, service.state.value)
        service.submitFeedback(CreateFocusTime(duration = 2, feedback = "From screen", finishedAt = 0))
        assertEquals(listOf("From screen"), fixture.focusSessionDataService.getAllFocusTimes().map { it.feedback })
    }

    private fun TestScope.finishInfiniteWork(service: TimerSessionService) {
        service.changeTimerSettings(INFINITE_TIMER_SETTINGS)
        service.startTimer()
        advanceTimeBy(2_000)
        runCurrent()
        service.stopTimer()
        runCurrent()
    }

    private fun createFixture(testScheduler: TestCoroutineScheduler): TimerSessionFixture {
        val dispatchers = AppDispatchers(
            io = StandardTestDispatcher(testScheduler),
            default = StandardTestDispatcher(testScheduler)
        )
        val database = Database(TimerSessionInMemoryDriverFactory())
        val workspaceSessionService = createWorkspaceSessionService(database)
        val focusSessionDataService =
            FocusSessionDataService(database, dispatchers, workspaceSessionService)
        val timerSettingsService = TimerSettingsService(database, workspaceSessionService)
        val focusTaskService = FocusTaskService()
        val notifications = FakeNotificationService()
        val timerSessionService = TimerSessionService(
            notificationService = notifications,
            focusSessionDataService = focusSessionDataService,
            timerSettingsService = timerSettingsService,
            focusTaskService = focusTaskService,
            dispatchers = dispatchers
        )

        return TimerSessionFixture(timerSessionService, notifications, focusSessionDataService)
    }
}

private data class TimerSessionFixture(
    val timerSessionService: TimerSessionService,
    val notifications: FakeNotificationService,
    val focusSessionDataService: FocusSessionDataService,
)

private class TimerSessionInMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}
