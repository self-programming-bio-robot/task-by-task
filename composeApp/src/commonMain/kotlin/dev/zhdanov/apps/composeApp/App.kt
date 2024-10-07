package dev.zhdanov.apps.composeApp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import dev.zhdanov.apps.composeApp.navigation.SetupNavGraph
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    val daySummaryService = koinInject<DaySummaryService>()

    LaunchedEffect(Unit) {
        daySummaryService.migration()
    }

    MaterialTheme {
        KoinContext {
            val navController = rememberNavController()
            SetupNavGraph(navController = navController)
        }
    }
}
