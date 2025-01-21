package dev.zhdanov.apps.composeApp.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.sharp.Timer
import androidx.compose.material3.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.components.settings.timers.TimersSettings
import dev.zhdanov.apps.composeApp.components.settings.timers.editor.EditableTimerSettings
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            defaultPanePreferredWidth = 300.dp,
        )
    )

    val windowInfo = currentWindowAdaptiveInfo()
    val coroutineScope = rememberCoroutineScope()
    val padding = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) 0.dp else 16.dp
    val shape = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT)
        RectangleShape else MaterialTheme.shapes.medium

    Scaffold(
        topBar = {
            TopBar(
                title = "Settings",
                hasBack = navigator.canNavigateBack(),
                onBack = { navigator.navigateBack() }
            )
        },
        content = { paddings ->
            Box(modifier = Modifier.padding(paddings)) {
                ListDetailPaneScaffold(
                    modifier = Modifier
                        .padding(start = padding, end = padding, bottom = padding),
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    listPane = {
                        AnimatedPane(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = shape
                                )
                        ) {
                            SettingList(
                                onItemClick = { item ->
                                    coroutineScope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, item)
                                    }
                                },
                            )
                        }
                    },
                    detailPane = {
                        AnimatedPane(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = shape
                                )
                        ) {
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
                        AnimatedPane(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = shape
                                )
                        ) {
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
        }
    )
}

@Composable
fun SettingList(onItemClick: (item: TimersSettingsProps) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
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
    Box(modifier = Modifier
        .fillMaxSize()
        .clip(MaterialTheme.shapes.extraLarge)
        .clickable {
            onItemClick(TimersSettingsProps())
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

data class TimersSettingsProps(
    val selectedItem: TimerSettings? = null,
)
