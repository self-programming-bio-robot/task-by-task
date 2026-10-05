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
    data object SettingsGeneral : Screen("General")

    @Serializable
    data object SettingsSecurity : Screen("Security")

    @Serializable
    data object SettingsTimers : Screen("Timers")

    @Serializable
    data class SettingsTimerEdit(
        val timerId: Long? = null
    ) : Screen("Timer")

    @Serializable
    data class FinishedDay(
        val date: LocalDate,
        val summary: String,
        val response: String
    ) : Screen("Finish day", Icons.AutoMirrored.Sharp.FactCheck)

    @Serializable
    data object TodayTasks : Screen("For today")

    @Serializable
    data class TaskList(
        val initialTaskId: Long? = null
    ) : Screen("Tasks", Icons.AutoMirrored.Sharp.ListAlt)

    @Serializable
    data class TaskEdit(
        val taskId: Long
    ) : Screen("Edit task")

    @Serializable
    data class Feedback(
        val duration: Int,
        val finishAt: Long
    ) : Screen("Feedback", Icons.AutoMirrored.Sharp.FactCheck)

    @Serializable
    data object Statistics : Screen("Statistics", Icons.Sharp.BarChart)

    @Serializable
    data class DayDetail(
        val date: LocalDate
    ) : Screen("Day Detail", Icons.Sharp.HistoryEdu)

    /**
     * Top-level menu item this screen belongs to.
     */
    fun menuScreen(): Screen = when (this) {
        is SettingsGeneral, is SettingsSecurity, is SettingsTimers, is SettingsTimerEdit -> Settings
        is DayDetail, is FinishedDay -> History
        is TodayTasks, is Feedback -> Home
        is TaskEdit -> TaskList()
        else -> this
    }
}
