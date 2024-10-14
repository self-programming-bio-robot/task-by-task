package dev.zhdanov.apps.composeApp.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String) {
    @Serializable
    object Home : Screen("home")

    @Serializable
    object History : Screen("history")

    @Serializable
    object Settings : Screen("settings")

    @Serializable
    data class FinishedDay(val summary: String, val response: String) : Screen("finish-day")
}
