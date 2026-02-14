package dev.zhdanov.apps.composeApp.components.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.navigation.MainNavGraph
import dev.zhdanov.apps.composeApp.navigation.NavigationViewModel
import dev.zhdanov.apps.composeApp.navigation.Screen

val menuItems = listOf(
    Screen.Home,
    Screen.Settings,
    Screen.History,
    Screen.TaskList
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveLayout() {
    val windowInfo = currentWindowAdaptiveInfo()
    val viewModel: NavigationViewModel = viewModel { NavigationViewModel() }

    when (windowInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            NavigationBarLayout(
                menuItems = menuItems,
                viewModel = viewModel,
            ) {
                MainNavGraph(viewModel = viewModel)
            }
        }

        else -> {
            NavigationRailLayout(
                menuItems = menuItems,
                viewModel = viewModel,
            ) {
                MainNavGraph(viewModel = viewModel)
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

    Row {
        NavigationRail(
            header = {
                Icon(
                    modifier = Modifier.padding(16.dp),
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Task by Task"
                )
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

        content()
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
        menuItems.indexOfFirst { it == currentKey }.takeIf { it >= 0 } ?: 0
    }
}
