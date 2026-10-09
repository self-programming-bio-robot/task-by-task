package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberSupportingPaneSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dev.zhdanov.apps.composeApp.components.layout.AppLayoutMode
import dev.zhdanov.apps.composeApp.components.layout.SceneScaffold
import dev.zhdanov.apps.composeApp.components.layout.rememberAppLayoutMode
import dev.zhdanov.apps.composeApp.components.pane.AppPane
import dev.zhdanov.apps.composeApp.components.settings.general.GeneralSettings
import dev.zhdanov.apps.composeApp.components.settings.security.SecuritySettings
import dev.zhdanov.apps.composeApp.components.settings.timers.TimersSettings
import dev.zhdanov.apps.composeApp.components.settings.timers.editor.EditableTimerSettings
import dev.zhdanov.apps.composeApp.components.topBar.RegisterTopBarActions
import dev.zhdanov.apps.composeApp.screens.finishedDay.FinishedDayScreen
import dev.zhdanov.apps.composeApp.screens.history.DayDetailScreen
import dev.zhdanov.apps.composeApp.screens.history.HistoryScreen
import dev.zhdanov.apps.composeApp.screens.home.HomeScreen
import dev.zhdanov.apps.composeApp.screens.home.TodayTasksPane
import dev.zhdanov.apps.composeApp.screens.settings.SettingsDetailPlaceholder
import dev.zhdanov.apps.composeApp.screens.settings.SettingsListPane
import dev.zhdanov.apps.composeApp.screens.statistics.StatisticsScreen
import dev.zhdanov.apps.composeApp.screens.tasks.TaskEditPane
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListScreen

private const val SETTINGS_SCENE_KEY = "settings"
private const val HOME_SCENE_KEY = "home"
private const val HISTORY_SCENE_KEY = "history"
private const val TASKS_SCENE_KEY = "tasks"

private val settingsDetailKeys = listOf<NavKey>(
    Screen.SettingsGeneral,
    Screen.SettingsSecurity,
    Screen.SettingsTimers,
)

private fun breadcrumbTitle(screen: Screen): String = when (screen) {
    is Screen.SettingsTimerEdit -> if (screen.timerId == null) "New timer" else "Edit timer"
    is Screen.DayDetail -> "Day ${screen.date}"
    else -> screen.title
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavGraph(
    viewModel: NavigationViewModel
) {
    // In Compact and Medium modes only one content pane is visible at a time;
    // Expanded allows a second pane (list+detail, main+supporting or detail+extra).
    val layoutMode = rememberAppLayoutMode()
    val singlePane = layoutMode != AppLayoutMode.Expanded

    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2()).copy(
        maxHorizontalPartitions = if (singlePane) 1 else 2,
        defaultPanePreferredWidth = 300.dp,
        horizontalPartitionSpacerSize = 16.dp,
    )
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = true,
        directive = directive,
    )
    val supportingPaneStrategy = rememberSupportingPaneSceneStrategy<NavKey>(
        shouldHandleSinglePaneLayout = true,
        directive = directive,
    )

    // Breadcrumbs are built from the contiguous suffix of the back stack that
    // belongs to the same top-level menu screen as the current key.
    val topKey = viewModel.backStack.lastOrNull()
    val topMenu = (topKey as? Screen)?.menuScreen()
    val sceneStack = viewModel.backStack
        .filterIsInstance<Screen>()
        .takeLastWhile { it.menuScreen() == topMenu }

    SceneScaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        title = sceneStack.joinToString(" / ") { breadcrumbTitle(it) },
        hasBack = sceneStack.size > 1,
        onBack = { viewModel.goBack() },
        topKey = topKey,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                     end = if (layoutMode == AppLayoutMode.Compact) 0.dp else 16.dp
                )
        ) {
            NavDisplay(
                backStack = viewModel.backStack,
                onBack = { viewModel.goBack() },
                sceneStrategies = listOf(listDetailStrategy, supportingPaneStrategy),
                entryProvider = { key ->
                    when (key) {
                        is Screen.Home -> NavEntry(
                            key as NavKey,
                            metadata = SupportingPaneSceneStrategy.mainPane(HOME_SCENE_KEY)
                        ) {
                            HomeScreen(
                                onFinishDay = { review ->
                                    viewModel.navigateTo(
                                        Screen.FinishedDay(
                                            date = review.date,
                                            summary = review.summary,
                                            response = review.response
                                        )
                                    )
                                },
                                onOpenTodayTasks = { viewModel.openTodayTasks() }
                            )
                        }

                        is Screen.TodayTasks -> NavEntry(
                            key as NavKey,
                            metadata = SupportingPaneSceneStrategy.supportingPane(HOME_SCENE_KEY)
                        ) {
                            TodayTasksPane(
                                onTaskFocused = { viewModel.goBack() }
                            )
                        }

                        is Screen.History -> NavEntry(
                            key as NavKey,
                            metadata = SupportingPaneSceneStrategy.mainPane(HISTORY_SCENE_KEY)
                        ) {
                            HistoryScreen(
                                onDayClick = { viewModel.openDayDetail(it) }
                            )
                        }

                        is Screen.DayDetail -> NavEntry(
                            key as NavKey,
                            metadata = SupportingPaneSceneStrategy.supportingPane(HISTORY_SCENE_KEY)
                        ) {
                            DayDetailScreen(
                                date = key.date,
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
                            AppPane(
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
                            AppPane(
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
                            AppPane(
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
                            RegisterTopBarActions(key) {
                                FilledTonalIconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { viewModel.openTimerEditor(null) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "New Timer",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            AppPane(
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
                            AppPane(
                                title = if (key.timerId == null) "New timer" else "Edit timer",
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                EditableTimerSettings(
                                    timerId = key.timerId,
                                    onBack = { viewModel.goBack() }
                                )
                            }
                        }

                        is Screen.TaskList -> NavEntry(
                            key as NavKey,
                            metadata = SupportingPaneSceneStrategy.mainPane(TASKS_SCENE_KEY)
                        ) {
                            TaskListScreen(
                                initialTaskId = key.initialTaskId,
                                onNavigateToTimer = {
                                    viewModel.navigateTo(Screen.Home)
                                },
                                onTaskClick = { viewModel.openTaskDetails(it) }
                            )
                        }

                        is Screen.TaskEdit -> NavEntry(
                            key as NavKey,
                            metadata = SupportingPaneSceneStrategy.supportingPane(TASKS_SCENE_KEY)
                        ) {
                            TaskEditPane(
                                taskId = key.taskId,
                                onDone = { viewModel.goBack() }
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
