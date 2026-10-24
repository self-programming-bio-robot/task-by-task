package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.SectionDetailPlaceholder
import dev.zhdanov.apps.composeApp.components.pane.SectionListItem
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.testing.UiTestTags

/**
 * List pane of the tasks list-detail scene. Mirrors SettingsListPane and StatisticsListPane.
 */
@Composable
fun TasksListPane(
    selected: Screen?,
    onItemClick: (Screen) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .testTag(UiTestTags.TasksListPane)
    ) {
        item {
            SectionListItem(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                title = Screen.TaskList().title,
                selected = selected is Screen.TaskList,
                onItemClick = { onItemClick(Screen.TaskList()) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.TaskList().title))
            )
        }
        item {
            SectionListItem(
                icon = Icons.Outlined.Repeat,
                title = Screen.TaskRoutines.title,
                selected = selected == Screen.TaskRoutines,
                onItemClick = { onItemClick(Screen.TaskRoutines) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.TaskRoutines.title))
            )
        }
    }
}

/**
 * Shown in the detail pane when no tasks section is selected yet.
 */
@Composable
fun TasksDetailPlaceholder() {
    SectionDetailPlaceholder(text = "Select a tasks section")
}
