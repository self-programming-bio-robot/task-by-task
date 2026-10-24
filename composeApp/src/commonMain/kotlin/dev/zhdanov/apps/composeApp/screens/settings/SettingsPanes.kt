package dev.zhdanov.apps.composeApp.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.SectionDetailPlaceholder
import dev.zhdanov.apps.composeApp.components.pane.SectionListItem
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.testing.UiTestTags

/**
 * List pane of the settings list-detail scene. The selected section key is
 * highlighted when the detail pane is shown next to this pane.
 */
@Composable
fun SettingsListPane(
    selected: Screen?,
    onItemClick: (Screen) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp)
    ) {
        item {
            SectionListItem(
                icon = Icons.Outlined.Settings,
                title = Screen.SettingsGeneral.title,
                selected = selected == Screen.SettingsGeneral,
                onItemClick = { onItemClick(Screen.SettingsGeneral) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.SettingsGeneral.title))
            )
        }
        item {
            SectionListItem(
                icon = Icons.Outlined.Security,
                title = Screen.SettingsSecurity.title,
                selected = selected == Screen.SettingsSecurity,
                onItemClick = { onItemClick(Screen.SettingsSecurity) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.SettingsSecurity.title))
            )
        }
        item {
            SectionListItem(
                icon = Icons.Sharp.Timer,
                title = Screen.SettingsTimers.title,
                selected = selected == Screen.SettingsTimers,
                onItemClick = { onItemClick(Screen.SettingsTimers) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.SettingsTimers.title))
            )
        }
    }
}

/**
 * Shown in the detail pane when no settings section is selected yet.
 */
@Composable
fun SettingsDetailPlaceholder() {
    SectionDetailPlaceholder(text = "Select a settings section")
}
