package dev.zhdanov.apps.composeApp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.zhdanov.apps.composeApp.components.layout.AdaptiveLayout
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    val daySummaryService = koinInject<DaySummaryService>()
    val timerSettingsService = koinInject<TimerSettingsService>()

    LaunchedEffect(Unit) {
        daySummaryService.migration()
        timerSettingsService.migration()
    }

    MaterialTheme {
        KoinContext {
            AdaptiveLayout()
        }
    }
}
