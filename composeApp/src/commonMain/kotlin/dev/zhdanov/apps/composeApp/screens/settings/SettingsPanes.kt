package dev.zhdanov.apps.composeApp.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.settings.SettingsPaneSurface
import dev.zhdanov.apps.composeApp.navigation.Screen

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
            SettingItem(
                icon = Icons.Outlined.Settings,
                title = Screen.SettingsGeneral.title,
                selected = selected == Screen.SettingsGeneral,
                onItemClick = { onItemClick(Screen.SettingsGeneral) }
            )
        }
        item {
            SettingItem(
                icon = Icons.Outlined.Security,
                title = Screen.SettingsSecurity.title,
                selected = selected == Screen.SettingsSecurity,
                onItemClick = { onItemClick(Screen.SettingsSecurity) }
            )
        }
        item {
            SettingItem(
                icon = Icons.Sharp.Timer,
                title = Screen.SettingsTimers.title,
                selected = selected == Screen.SettingsTimers,
                onItemClick = { onItemClick(Screen.SettingsTimers) }
            )
        }
    }
}

/**
 * Shown in the detail pane when no settings section is selected yet.
 */
@Composable
fun SettingsDetailPlaceholder() {
    SettingsPaneSurface(color = MaterialTheme.colorScheme.primaryContainer) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a settings section",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onItemClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
            )
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
