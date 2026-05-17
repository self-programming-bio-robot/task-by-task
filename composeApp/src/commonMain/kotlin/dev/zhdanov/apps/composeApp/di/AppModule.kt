package dev.zhdanov.apps.composeApp.di

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
import dev.zhdanov.apps.composeApp.services.DaySummaryDataService
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.FocusSessionDataService
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.services.HistoryService
import dev.zhdanov.apps.composeApp.services.OpenAIChatClient
import dev.zhdanov.apps.composeApp.services.OpenAIReviewClient
import dev.zhdanov.apps.composeApp.services.ReviewClient
import dev.zhdanov.apps.composeApp.services.StatisticsDataService
import dev.zhdanov.apps.composeApp.services.TaskDataService
import dev.zhdanov.apps.composeApp.services.TimerSessionService
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import org.koin.core.module.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule = module {
    single { AppDispatchers() }
    single { NotificationService() }
    single { WorkspaceSessionService(get(), get()) }
    single { AppSettingsService(get(), get(), get()) }
    single { TaskDataService(get(), get(), get()) }
    single { FocusSessionDataService(get(), get(), get()) }
    single { DaySummaryDataService(get(), get(), get()) }
    single { StatisticsDataService(get(), get()) }
    single { HistoryService(get(), get(), get(), get()) }
    single<ReviewClient> { OpenAIReviewClient() }
    single<ChatClient> { OpenAIChatClient() }
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

expect val platformModule: Module

fun initializeKoin() {
    startKoin {
        modules(appModule, platformModule)
    }
}
