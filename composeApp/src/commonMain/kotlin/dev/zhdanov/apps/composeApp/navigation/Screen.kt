package dev.zhdanov.apps.composeApp.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object History : Screen("history")

    data class FinishedDay(val reviewId: String) : Screen("profile/$reviewId")
}
