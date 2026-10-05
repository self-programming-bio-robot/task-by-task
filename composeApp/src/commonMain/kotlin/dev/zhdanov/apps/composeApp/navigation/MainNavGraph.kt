package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dev.zhdanov.apps.composeApp.components.layout.AppLayoutMode
import dev.zhdanov.apps.composeApp.components.layout.rememberAppLayoutMode
import dev.zhdanov.apps.composeApp.components.settings.SettingsPane
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.composeApp.components.settings.general.GeneralSettings
import dev.zhdanov.apps.composeApp.components.settings.security.SecuritySettings
import dev.zhdanov.apps.composeApp.components.settings.timers.TimersSettings
import dev.zhdanov.apps.composeApp.components.settings.timers.editor.EditableTimerSettings
import dev.zhdanov.apps.composeApp.screens.finishedDay.FinishedDayScreen
import dev.zhdanov.apps.composeApp.screens.history.DayDetailScreen
import dev.zhdanov.apps.composeApp.screens.history.HistoryScreen
import dev.zhdanov.apps.composeApp.screens.home.HomeScreen
import dev.zhdanov.apps.composeApp.screens.settings.SettingsDetailPlaceholder
import dev.zhdanov.apps.composeApp.screens.settings.SettingsListPane
import dev.zhdanov.apps.composeApp.screens.statistics.StatisticsScreen
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListScreen

private const val SETTINGS_SCENE_KEY = "settings"

private val settingsDetailKeys = listOf<NavKey>(
    Screen.SettingsGeneral,
    Screen.SettingsSecurity,
    Screen.SettingsTimers,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavGraph(
    viewModel: NavigationViewModel
) {
    // In Compact and Medium modes only one content pane is visible at a time;
    // Expanded allows a second pane (list+detail or detail+extra).
    val layoutMode = rememberAppLayoutMode()
    val singlePane = layoutMode != AppLayoutMode.Expanded

    // Settings entries form a single scene; the scene-level top bar renders
    // the screen title as breadcrumbs built from the back stack.
    val topKey = viewModel.backStack.lastOrNull()
    val inSettings = (topKey as? Screen)?.menuScreen() == Screen.Settings

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = true,
        directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).copy(
            maxHorizontalPartitions = if (singlePane) 1 else 2,
            defaultPanePreferredWidth = 300.dp,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (inSettings) {
            val settingsStack = viewModel.backStack
                .filterIsInstance<Screen>()
                .filter { it.menuScreen() == Screen.Settings }
            TopBar(
                title = settingsStack.joinToString(" / ") { screen ->
                    if (screen is Screen.SettingsTimerEdit) {
                        if (screen.timerId == null) "New timer" else "Edit timer"
                    } else {
                        screen.title
                    }
                },
                hasBack = settingsStack.size > 1,
                onBack = { viewModel.goBack() },
                actions = {
                    if (topKey is Screen.SettingsTimers) {
                        FilledTonalIconButton(
                            onClick = { viewModel.openTimerEditor(null) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "New Timer"
                            )
                        }
                    }
                }
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (inSettings && layoutMode != AppLayoutMode.Compact)
                        Modifier.padding(horizontal = 16.dp)
                    else Modifier
                )
        ) {
            NavDisplay(
                backStack = viewModel.backStack,
                onBack = { viewModel.goBack() },
                sceneStrategies = listOf(listDetailStrategy),
                entryProvider = { key ->
                    when (key) {
                        is Screen.Home -> NavEntry(key as NavKey) {
                            HomeScreen(
                                onFinishDay = { review ->
                                    viewModel.navigateTo(
                                        Screen.FinishedDay(
                                            date = review.date,
                                            summary = review.summary,
                                            response = review.response
                                        )
                                    )
                                }
                            )
                        }

                        is Screen.History -> NavEntry(key as NavKey) {
                            HistoryScreen(
                                onNavigateToTask = { taskId ->
                                    viewModel.navigateTo(Screen.TaskList(initialTaskId = taskId))
                                },
                                onNavigateToDayDetail = { date ->
                                    viewModel.navigateTo(Screen.DayDetail(date = date))
                                }
                            )
                        }

                        is Screen.DayDetail -> NavEntry(key as NavKey) {
                            DayDetailScreen(
                                date = key.date,
                                onBack = { viewModel.goBack() },
                                onNavigateToTask = { taskId ->
                                    viewModel.navigateTo(Screen.TaskList(initialTaskId = taskId))
                                }
                            )
                        }

                        is Screen.FinishedDay -> NavEntry(key as NavKey) {
                            FinishedDayScreen(
                                date = key.date,
                                summary = key.summary,
                                response = key.response,
                                onNext = {
                                    viewModel.popUpTo(key, inclusive = true)
                                    viewModel.navigateTo(Screen.History)
                                }
                            )
                        }

                        is Screen.Settings -> NavEntry(
                            key as NavKey,
                            metadata = ListDetailSceneStrategy.listPane(
                                sceneKey = SETTINGS_SCENE_KEY,
                                detailPlaceholder = { SettingsDetailPlaceholder() },
                            )
                        ) {
                            SettingsPane(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                SettingsListPane(
                                    selected = viewModel.backStack.lastOrNull { it in settingsDetailKeys } as? Screen,
                                    onItemClick = { viewModel.selectSettingsSection(it) },
                                )
                            }
                        }

                        is Screen.SettingsGeneral -> NavEntry(
                            key as NavKey,
                            metadata = ListDetailSceneStrategy.detailPane(sceneKey = SETTINGS_SCENE_KEY)
                        ) {
                            SettingsPane(
                                title = key.title,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                GeneralSettings()
                            }
                        }

                        is Screen.SettingsSecurity -> NavEntry(
                            key as NavKey,
                            metadata = ListDetailSceneStrategy.detailPane(sceneKey = SETTINGS_SCENE_KEY)
                        ) {
                            SettingsPane(
                                title = key.title,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                SecuritySettings()
                            }
                        }

                        is Screen.SettingsTimers -> NavEntry(
                            key as NavKey,
                            metadata = ListDetailSceneStrategy.detailPane(sceneKey = SETTINGS_SCENE_KEY)
                        ) {
                            SettingsPane(
                                title = key.title,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                TimersSettings(
                                    onItemClick = { timer -> viewModel.openTimerEditor(timer.id) }
                                )
                            }
                        }

                        is Screen.SettingsTimerEdit -> NavEntry(
                            key as NavKey,
                            metadata = ListDetailSceneStrategy.extraPane(sceneKey = SETTINGS_SCENE_KEY)
                        ) {
                            SettingsPane(
                                title = if (key.timerId == null) "New timer" else "Edit timer",
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                EditableTimerSettings(
                                    timerId = key.timerId,
                                    onBack = { viewModel.goBack() }
                                )
                            }
                        }

                        is Screen.TaskList -> NavEntry(key as NavKey) {
                            TaskListScreen(
                                initialTaskId = key.initialTaskId,
                                onNavigateToTimer = {
                                    viewModel.navigateTo(Screen.Home)
                                }
                            )
                        }

                        is Screen.Statistics -> NavEntry(key as NavKey) {
                            StatisticsScreen()
                        }

                        else -> NavEntry(key) {
                            // Unknown route
                        }
                    }
                }
            )
        }
    }
}
