package dev.zhdanov.apps.composeApp.ui

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.composeApp.components.settings.general.GeneralSettingsViewModel
import dev.zhdanov.apps.composeApp.components.settings.security.SecuritySettingsViewModel
import dev.zhdanov.apps.composeApp.components.settings.timers.TimersSettingsViewModel
import dev.zhdanov.apps.composeApp.components.settings.timers.editor.EditableTimerSettingsViewModel
import dev.zhdanov.apps.composeApp.components.timer.TimerViewModel
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.screens.finishedDay.FinishedDayViewModel
import dev.zhdanov.apps.composeApp.screens.history.HistoryViewModel
import dev.zhdanov.apps.composeApp.screens.home.HomeViewModel
import dev.zhdanov.apps.composeApp.screens.statistics.StatisticsViewModel
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListViewModel
import dev.zhdanov.apps.composeApp.services.AppDispatchers
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import dev.zhdanov.apps.composeApp.services.ChatClient
import dev.zhdanov.apps.composeApp.services.ChatService
import dev.zhdanov.apps.composeApp.services.DayReviewResult
import dev.zhdanov.apps.composeApp.services.DaySummaryDataService
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.FocusSessionDataService
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.services.HistoryService
import dev.zhdanov.apps.composeApp.services.JvmWorkspaceCryptoService
import dev.zhdanov.apps.composeApp.services.ReviewClient
import dev.zhdanov.apps.composeApp.services.SchedulerAction
import dev.zhdanov.apps.composeApp.services.SchedulerService
import dev.zhdanov.apps.composeApp.services.StatisticsDataService
import dev.zhdanov.apps.composeApp.services.TaskDataService
import dev.zhdanov.apps.composeApp.services.ThemeChangeService
import dev.zhdanov.apps.composeApp.services.TimerSessionService
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import dev.zhdanov.apps.composeApp.services.WorkspaceCryptoService
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.model.AssistantConfig
import dev.zhdanov.apps.shared.model.ChatMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
internal fun startDesktopUiTestKoin() {
    stopDesktopUiTestKoin()

    startKoin {
        modules(
            module {
                single { AppDispatchers(UnconfinedTestDispatcher(), UnconfinedTestDispatcher()) }
                single { NotificationService() }
                single<DatabaseDriverFactory> { InMemoryDriverFactory() }
                single { Database(get()) }
                single<SchedulerService> { NoopSchedulerService() }
                single<ThemeChangeService> { FakeThemeChangeService() }
                single<WorkspaceCryptoService> { JvmWorkspaceCryptoService() }
                single { WorkspaceSessionService(get(), get()) }
                single { AppSettingsService(get(), get(), get()) }
                single { TaskDataService(get(), get(), get()) }
                single { FocusSessionDataService(get(), get(), get()) }
                single { DaySummaryDataService(get(), get(), get()) }
                single { StatisticsDataService(get(), get()) }
                single { HistoryService(get(), get(), get(), get()) }
                single<ReviewClient> { FakeReviewClient() }
                single<ChatClient> { FakeChatClient() }
                single { DaySummaryService(get(), get(), get(), get(), get(), get(), get()) }
                single { TimerSettingsService(get(), get()) }
                single { FocusTaskService() }
                single { ChatService(get(), get(), get()) }
                single { TimerSessionService(get(), get(), get(), get(), get()) }

                viewModel { TimerViewModel(get()) }
                viewModel { GeneralSettingsViewModel(get(), get(), get()) }
                viewModel { SecuritySettingsViewModel(get(), get()) }
                viewModel { FinishedDayViewModel(get()) }
                viewModel { StatisticsViewModel(get()) }
                viewModel { HomeViewModel(get()) }
                viewModel { HistoryViewModel(get()) }
                viewModel { TimersSettingsViewModel(get()) }
                viewModel { EditableTimerSettingsViewModel(get()) }
                viewModel { TaskListViewModel(get(), get()) }
            }
        )
    }
}

internal fun stopDesktopUiTestKoin() {
    runCatching { stopKoin() }
}

private class InMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}

@OptIn(ExperimentalTime::class)
private class NoopSchedulerService : SchedulerService {
    override fun addScheduler(tag: String, cron: String, timeZone: TimeZone, action: SchedulerAction) = Unit

    override fun addScheduler(tag: String, cron: String, action: SchedulerAction) = Unit
}

private class FakeThemeChangeService : ThemeChangeService {
    private val listeners = mutableSetOf<ThemeChangeService.ThemeChangeListener>()

    override fun registerListener(listener: ThemeChangeService.ThemeChangeListener) {
        listeners += listener
        listener.onThemeChanged(ThemeChangeService.SystemTheme.LIGHT)
    }

    override fun removeListener(listener: ThemeChangeService.ThemeChangeListener) {
        listeners -= listener
    }
}

private class FakeReviewClient : ReviewClient {
    override suspend fun reviewDay(config: AssistantConfig, historyOfDay: String): DayReviewResult {
        return DayReviewResult(summary = "summary", response = "response")
    }
}

private class FakeChatClient : ChatClient {
    override suspend fun sendMessage(config: AssistantConfig, daySummary: String, messages: List<ChatMessage>): String {
        return "fake reply"
    }
}
