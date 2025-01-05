package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.FactCheck
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.sharp.HistoryEdu
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material.icons.sharp.Task
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Screen(
    val title: String,
    @Transient val icon: ImageVector = Icons.Outlined.Close,
) {
    @Serializable
    object Home : Screen("Home", Icons.Sharp.Home)

    @Serializable
    object History : Screen("History", Icons.Sharp.HistoryEdu)

    @Serializable
    object Settings : Screen("Settings", Icons.Sharp.Settings)

    @Serializable
    data class FinishedDay(val summary: String, val response: String) : Screen("Finish day",
        Icons.AutoMirrored.Sharp.FactCheck
    )

    @Serializable
    object TaskList : Screen("Tasks", Icons.Sharp.Task)
}
