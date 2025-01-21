package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.FactCheck
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.sharp.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Screen(
    val title: String,
    @Transient val icon: ImageVector = Icons.Outlined.Close,
    val screenParts: Int = 1,
) {
    @Serializable
    data object Home : Screen("Home", Icons.Sharp.Home)

    @Serializable
    data object History : Screen("History", Icons.Sharp.HistoryEdu)

    @Serializable
    data object Settings : Screen("Settings", Icons.Sharp.Settings, 2)

    @Serializable
    data class FinishedDay(val summary: String, val response: String) : Screen("Finish day",
        Icons.AutoMirrored.Sharp.FactCheck
    )

    @Serializable
    data object TaskList : Screen("Tasks", Icons.Sharp.Task)

    @Serializable
    data object Empty : Screen("Empty", Icons.Sharp.FilterNone)
}

fun getScreenByName(name: String): Screen? {
    return when (name) {
        Screen.Home::class.qualifiedName -> Screen.Home
        Screen.History::class.qualifiedName -> Screen.History
        Screen.Settings::class.qualifiedName -> Screen.Settings
        Screen.TaskList::class.qualifiedName -> Screen.TaskList
        Screen.FinishedDay::class.qualifiedName -> Screen.FinishedDay("", "")
        else -> null
    }
}
