package dev.zhdanov.apps.composeApp.di

import dev.zhdanov.apps.composeApp.components.timer.TimerViewModel
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.screens.home.HomeViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module


val appModule = module {
    single { NotificationService() }

    viewModel { HomeViewModel() }
    viewModel { TimerViewModel(get(), get()) }
}

expect val platformModule: Module

fun initializeKoin() {
    startKoin {
        modules(appModule, platformModule)
    }
}