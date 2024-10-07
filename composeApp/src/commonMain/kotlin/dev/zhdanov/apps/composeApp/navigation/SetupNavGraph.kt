package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.zhdanov.apps.composeApp.screens.finishedDay.FinishedDayScreen
import dev.zhdanov.apps.composeApp.screens.history.HistoryScreen
import dev.zhdanov.apps.composeApp.screens.home.HomeScreen

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                navigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onFinishDay = { reviewId ->
                    navController.navigate(Screen.FinishedDay(reviewId).route)
                }
            )
        }
        composable(route = Screen.History.route) {
            HistoryScreen(onBack = {
                navController.popBackStack()
            })
        }
        composable(route = Screen.FinishedDay("{reviewId}").route,
            arguments = listOf(navArgument("reviewId") {
                type = NavType.StringType
                nullable = false
            })
        ) { backStackEntry ->
            val reviewId = backStackEntry.arguments?.getString("reviewId")
            FinishedDayScreen(reviewId) {
                navController.navigate(Screen.History.route) {
                    popUpTo(Screen.FinishedDay(reviewId ?: "").route) {
                        inclusive = true
                    }
                }
            }
        }
    }
}
