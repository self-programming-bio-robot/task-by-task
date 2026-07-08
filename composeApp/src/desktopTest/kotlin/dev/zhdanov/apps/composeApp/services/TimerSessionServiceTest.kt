package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.composeApp.components.timer.TimerViewState
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.shared.INFINITE_TIMER_SETTINGS
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    private fun createFixture(testScheduler: TestCoroutineScheduler): TimerSessionFixture {
        val dispatchers = AppDispatchers(
            io = StandardTestDispatcher(testScheduler),
            default = StandardTestDispatcher(testScheduler)
        )
        val database = Database(TimerSessionInMemoryDriverFactory())
        val workspaceSessionService = createWorkspaceSessionService(database)
        val focusSessionDataService = FocusSessionDataService(database, dispatchers, workspaceSessionService)
        val timerSettingsService = TimerSettingsService(database, workspaceSessionService)
        val focusTaskService = FocusTaskService()
        val timerSessionService = TimerSessionService(
            notificationService = NotificationService(),
            focusSessionDataService = focusSessionDataService,
            timerSettingsService = timerSettingsService,
            focusTaskService = focusTaskService,
            dispatchers = dispatchers
        )

        return TimerSessionFixture(timerSessionService)
    }
}

private data class TimerSessionFixture(
    val timerSessionService: TimerSessionService
)

private class TimerSessionInMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}
