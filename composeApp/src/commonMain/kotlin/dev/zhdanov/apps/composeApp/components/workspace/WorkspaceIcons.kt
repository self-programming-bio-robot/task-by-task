package dev.zhdanov.apps.composeApp.components.workspace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.ui.graphics.vector.ImageVector
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ICON

data class WorkspaceIconOption(
    val id: String,
    val title: String,
    val imageVector: ImageVector
)

val WorkspaceIconOptions = listOf(
    WorkspaceIconOption(DEFAULT_WORKSPACE_ICON, "Workspace", Icons.Outlined.Workspaces),
    WorkspaceIconOption("home", "Home", Icons.Outlined.Home),
    WorkspaceIconOption("folder", "Folder", Icons.Outlined.Folder),
    WorkspaceIconOption("code", "Code", Icons.Outlined.Code),
    WorkspaceIconOption("rocket", "Launch", Icons.Outlined.RocketLaunch),
    WorkspaceIconOption("lock", "Secure", Icons.Outlined.Lock),
    WorkspaceIconOption("business", "Business", Icons.Outlined.BusinessCenter),
    WorkspaceIconOption("team", "Team", Icons.Outlined.Groups),
    WorkspaceIconOption("tasks", "Tasks", Icons.Outlined.Checklist),
    WorkspaceIconOption("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    WorkspaceIconOption("timer", "Timer", Icons.Outlined.Timer),
    WorkspaceIconOption("idea", "Idea", Icons.Outlined.Lightbulb),
    WorkspaceIconOption("magic", "Magic", Icons.Outlined.AutoAwesome),
    WorkspaceIconOption("school", "Study", Icons.Outlined.School),
    WorkspaceIconOption("cloud", "Cloud", Icons.Outlined.Cloud),
    WorkspaceIconOption("shield", "Shield", Icons.Outlined.Shield),
    WorkspaceIconOption("terminal", "Terminal", Icons.Outlined.Terminal),
    WorkspaceIconOption("flag", "Flag", Icons.Outlined.Flag),
    WorkspaceIconOption("star", "Star", Icons.Outlined.Star),
    WorkspaceIconOption("palette", "Palette", Icons.Outlined.Palette),
    WorkspaceIconOption("brush", "Brush", Icons.Outlined.Brush),
    WorkspaceIconOption("explore", "Explore", Icons.Outlined.Explore),
    WorkspaceIconOption("mind", "Mind", Icons.Outlined.Psychology),
    WorkspaceIconOption("tree", "Tree", Icons.Outlined.AccountTree),
    WorkspaceIconOption("bookmark", "Bookmark", Icons.Outlined.Bookmark)
)

fun workspaceIconVector(iconId: String?): ImageVector =
    WorkspaceIconOptions.firstOrNull { it.id == iconId }?.imageVector
        ?: WorkspaceIconOptions.first().imageVector

fun workspaceIconTitle(iconId: String?): String =
    WorkspaceIconOptions.firstOrNull { it.id == iconId }?.title
        ?: WorkspaceIconOptions.first().title
