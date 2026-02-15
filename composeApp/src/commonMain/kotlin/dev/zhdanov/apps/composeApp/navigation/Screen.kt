package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.FactCheck
import androidx.compose.material.icons.automirrored.sharp.ListAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.sharp.HistoryEdu
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material.icons.sharp.BarChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Screen(
    val title: String,
    @Transient val icon: ImageVector = Icons.Outlined.Close,
) : NavKey {
    @Serializable
    data object Home : Screen("Home", Icons.Sharp.Home)

    @Serializable
    data object History : Screen("History", Icons.Sharp.HistoryEdu)

    @Serializable
    data object Settings : Screen("Settings", Icons.Sharp.Settings)

    @Serializable
    data class FinishedDay(
        val date: LocalDate,
        val summary: String,
        val response: String
    ) : Screen("Finish day", Icons.AutoMirrored.Sharp.FactCheck)

    @Serializable
    data class TaskList(
        val initialTaskId: Long? = null
    ) : Screen("Tasks", Icons.AutoMirrored.Sharp.ListAlt)

    @Serializable
    data class Feedback(
        val duration: Int,
        val finishAt: Long
    ) : Screen("Feedback", Icons.AutoMirrored.Sharp.FactCheck)

    @Serializable
    data object Statistics : Screen("Statistics", Icons.Sharp.BarChart)
}
