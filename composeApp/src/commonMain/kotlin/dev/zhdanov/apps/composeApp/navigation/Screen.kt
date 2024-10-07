package dev.zhdanov.apps.composeApp.navigation

import androidx.navigation.NavType
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String) {
    @Serializable
    object Home : Screen("home")

    @Serializable
    object History : Screen("history")

    @Serializable
    data class FinishedDay(val summary: String, val response: String) : Screen("finish-day")
}
