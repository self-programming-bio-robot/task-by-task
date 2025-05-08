package dev.zhdanov.apps.composeApp.di

import dev.zhdanov.apps.composeApp.components.settings.general.GeneralSettingsViewModel
import dev.zhdanov.apps.composeApp.components.settings.timers.TimersSettingsViewModel
import dev.zhdanov.apps.composeApp.components.settings.timers.editor.EditableTimerSettingsViewModel
import dev.zhdanov.apps.composeApp.components.timer.TimerViewModel
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.screens.history.HistoryViewModel
import dev.zhdanov.apps.composeApp.screens.home.HomeViewModel
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListViewModel
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module


val appModule = module {
    single { NotificationService() }
    single { DaySummaryService(get(), get()) }
    single { TimerSettingsService(get()) }
    single { TimerViewModel(get(), get(), get()) }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { TimersSettingsViewModel(get()) }
    viewModel { EditableTimerSettingsViewModel(get()) }
    viewModel { TaskListViewModel(get(), get()) }
    viewModel { GeneralSettingsViewModel(get()) }
}

expect val platformModule: Module

fun initializeKoin() {
    startKoin {
        modules(appModule, platformModule)
    }
}
