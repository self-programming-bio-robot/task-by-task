package dev.zhdanov.apps.composeApp.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.components.workspace.WorkspaceSelector
import dev.zhdanov.apps.composeApp.navigation.MainNavGraph
import dev.zhdanov.apps.composeApp.navigation.NavigationViewModel
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import org.koin.compose.koinInject

val menuItems: List<Screen> = listOf(
    Screen.Home,
    Screen.Statistics,
    Screen.History,
    Screen.TaskList(),
    Screen.Settings
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveLayout() {
    val windowInfo = currentWindowAdaptiveInfo()
    val viewModel: NavigationViewModel = viewModel { NavigationViewModel() }
    val workspaceSessionService: WorkspaceSessionService = koinInject()
    val currentWorkspace by workspaceSessionService.currentWorkspace.collectAsState()

    when (windowInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            NavigationBarLayout(
                menuItems = menuItems,
                viewModel = viewModel,
            ) {
                Column {
                    WorkspaceSelector(
                        expandedContent = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        key(currentWorkspace?.id) {
                            MainNavGraph(viewModel = viewModel)
                        }
                    }
                }
            }
        }

        else -> {
            NavigationRailLayout(
                menuItems = menuItems,
                viewModel = viewModel,
            ) {
                key(currentWorkspace?.id) {
                    MainNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NavigationRailLayout(
    modifier: Modifier = Modifier,
    menuItems: List<Screen> = listOf(),
    viewModel: NavigationViewModel,
    content: @Composable () -> Unit,
) {
    val currentKey = viewModel.backStack.lastOrNull()
    val selectedIndex = rememberSelectedIndex(menuItems, currentKey)

    Row(modifier = modifier.fillMaxSize()) {
        NavigationRail(
            header = {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WorkspaceSelector(expandedContent = false)
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                menuItems.forEachIndexed { index, item ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selectedIndex == index,
                        onClick = {
                            viewModel.navigateAndClear(item)
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavigationBarLayout(
    modifier: Modifier = Modifier,
    menuItems: List<Screen> = listOf(),
    viewModel: NavigationViewModel,
    content: @Composable () -> Unit,
) {
    val currentKey = viewModel.backStack.lastOrNull()
    val selectedIndex = rememberSelectedIndex(menuItems, currentKey)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
            ) {
                menuItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selectedIndex == index,
                        onClick = {
                            viewModel.navigateAndClear(item)
                        }
                    )
                }
            }
        },
        content = { paddings ->
            Box(modifier = Modifier.padding(paddings)) {
                content()
            }
        }
    )
}

@Composable
private fun rememberSelectedIndex(menuItems: List<Screen>, currentKey: NavKey?): Int {
    return remember(menuItems, currentKey) {
        menuItems.indexOfFirst { item ->
            when {
                item is Screen.TaskList && currentKey is Screen.TaskList -> true
                else -> item == currentKey
            }
        }.takeIf { it >= 0 } ?: 0
    }
}
