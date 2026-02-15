package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dev.zhdanov.apps.composeApp.screens.finishedDay.FinishedDayScreen
import dev.zhdanov.apps.composeApp.screens.history.HistoryScreen
import dev.zhdanov.apps.composeApp.screens.home.HomeScreen
import dev.zhdanov.apps.composeApp.screens.settings.SettingsScreen
import dev.zhdanov.apps.composeApp.screens.statistics.StatisticsScreen
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavGraph(
    viewModel: NavigationViewModel
) {
    NavDisplay(
        backStack = viewModel.backStack,
        onBack = { viewModel.goBack() },
        entryProvider = { key ->
            when (key) {
                is Screen.Home -> NavEntry(key as NavKey) {
                    HomeScreen(
                        onFinishDay = { review ->
                            viewModel.navigateTo(Screen.FinishedDay(
                                date = review.date,
                                summary = review.summary,
                                response = review.response
                            ))
                        }
                    )
                }

                is Screen.History -> NavEntry(key as NavKey) {
                    HistoryScreen(
                        onNavigateToTask = { taskId ->
                            viewModel.navigateTo(Screen.TaskList(initialTaskId = taskId))
                        }
                    )
                }

                is Screen.FinishedDay -> NavEntry(key as NavKey) {
                    FinishedDayScreen(
                        date = key.date,
                        summary = key.summary,
                        response = key.response,
                        onNext = {
                            viewModel.popUpTo(key, inclusive = true)
                            viewModel.navigateTo(Screen.History)
                        }
                    )
                }

                is Screen.Settings -> NavEntry(key as NavKey) {
                    SettingsScreen(
                        onBack = {
                            viewModel.goBack()
                        }
                    )
                }

                is Screen.TaskList -> NavEntry(key as NavKey) {
                    TaskListScreen(
                        initialTaskId = key.initialTaskId,
                        onNavigateToTimer = {
                            viewModel.navigateTo(Screen.Home)
                        }
                    )
                }

                is Screen.Statistics -> NavEntry(key as NavKey) {
                    StatisticsScreen()
                }

                else -> NavEntry(Unit as NavKey) {
                    // Unknown route
                }
            }
        }
    )
}
