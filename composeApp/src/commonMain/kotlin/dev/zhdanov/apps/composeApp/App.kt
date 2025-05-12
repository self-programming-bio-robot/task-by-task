package dev.zhdanov.apps.composeApp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.zhdanov.apps.composeApp.components.layout.AdaptiveLayout
import dev.zhdanov.apps.composeApp.components.settings.general.GeneralSettingsViewModel
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
    val generalSettingsViewModel: GeneralSettingsViewModel = koinInject()

    val theme by generalSettingsViewModel.theme.collectAsState()
    val colors = when (theme) {
        "dark" -> darkColorScheme()
        "light" -> lightColorScheme()
        else -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    }

    LaunchedEffect(Unit) {
        daySummaryService.migration()
        timerSettingsService.migration()
    }
    MaterialTheme(
        colorScheme = colors
    ) {
        KoinContext {
            AdaptiveLayout()
        }
    }
}
