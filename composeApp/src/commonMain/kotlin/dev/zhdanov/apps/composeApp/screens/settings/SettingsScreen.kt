package dev.zhdanov.apps.composeApp.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.settings.timers.TimersSettings
import dev.zhdanov.apps.composeApp.components.settings.timers.editor.EditableTimerSettings
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            defaultPanePreferredWidth = 300.dp,
        )
    )

    val coroutineScope = rememberCoroutineScope()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.padding(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Settings", style = MaterialTheme.typography.headlineMedium)
                    }
                    SettingList(
                        onItemClick = { item ->
                            coroutineScope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                            }
                        },
                    )
                }
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.padding(16.dp)) {
                navigator.currentDestination?.contentKey?.let {
                    when {
                        it is TimersSettingsProps -> TimersSettings(
                            onItemClick = { timer ->
                                coroutineScope.launch {
                                    navigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Extra,
                                        TimersSettingsProps(timer)
                                    )
                                }
                            },
                            onCreate = {
                                coroutineScope.launch {
                                    navigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Extra,
                                        TimersSettingsProps(null)
                                    )
                                }
                            },
                            onBack = {
                                coroutineScope.launch { navigator.navigateBack() }
                            }
                        )
                    }
                }
            }
        },
        extraPane = {
            AnimatedPane(modifier = Modifier.padding(16.dp)) {
                navigator.currentDestination?.contentKey?.let {
                    when {
                        it is TimersSettingsProps ->
                            EditableTimerSettings(
                                timerSettings = it.selectedItem,
                                onBack = {
                                    coroutineScope.launch { navigator.navigateBack() }
                                }
                            )
                    }
                }
            }
        }
    )
}

@Composable
fun SettingList(onItemClick: (item: TimersSettingsProps) -> Unit) {
    LazyColumn(
        modifier = Modifier
    ) {
        item {
            SettingItem(
                icon = Icons.Sharp.Timer,
                title = "Timers",
                onItemClick
            )
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    onItemClick: (item: TimersSettingsProps) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                onItemClick(TimersSettingsProps())
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier
                    .padding(8.dp)
                    .padding(start = 16.dp),
                imageVector = icon,
                contentDescription = title,
                tint = Color.Black
            )

            Text(
                modifier = Modifier
                    .padding(8.dp),
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }

    }
}

data class TimersSettingsProps(
    val selectedItem: TimerSettings? = null,
)
