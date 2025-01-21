package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.zhdanov.apps.composeApp.screens.finishedDay.FinishedDayScreen
import dev.zhdanov.apps.composeApp.screens.history.HistoryScreen
import dev.zhdanov.apps.composeApp.screens.home.HomeScreen
import dev.zhdanov.apps.composeApp.screens.settings.SettingsScreen
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavGraph(
    navController: NavHostController,
    startDestination: Screen = Screen.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onFinishDay = { review ->
                    navController.navigate(Screen.FinishedDay(review.summary, review.response))
                }
            )
        }

        composable<Screen.History> {
            HistoryScreen()
        }

        composable<Screen.FinishedDay> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.FinishedDay>()
            FinishedDayScreen(route.summary, route.response) {
                navController.navigate(Screen.History) {
                    popUpTo(Screen.FinishedDay(route.summary, route.response)) {
                        inclusive = true
                    }
                }
            }
        }

        composable<Screen.Settings> {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.TaskList> {
            TaskListScreen()
        }
    }
}
