package dev.zhdanov.apps.composeApp.components.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.navigation.SetupNavGraph

val menuItems = listOf(
    Screen.Home,
    Screen.Settings,
    Screen.History,
    Screen.TaskList
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveLayout(

) {
    val windowInfo = currentWindowAdaptiveInfo()
    val navController = rememberNavController()

    when (windowInfo.windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            NavigationBarLayout(
                menuItems = menuItems,
                navController = navController,
                mainPane = {
                    SetupNavGraph(navController = navController)
                },
                supportingPane = {},
                extraPane = {}
            )
        }

        else -> {
            NavigationRailLayout(
                menuItems = menuItems,
                navController = navController,
                mainPane = {
                    SetupNavGraph(navController = navController)
                },
                supportingPane = {},
                extraPane = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavigationRailLayout(
    modifier: Modifier = Modifier,
    mainPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    supportingPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    extraPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    menuItems: List<Screen> = listOf(),
    navController: NavHostController,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator<Screen>()
    var selectedItem by remember { mutableIntStateOf(0) }

    Row {
        NavigationRail {
            menuItems.forEachIndexed { index, item ->
                NavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title
                        )
                    },
                    label = { Text(item.title) },
                    selected = selectedItem == index,
                    onClick = {
                        selectedItem = index
                        navController.navigate(item)
                    }
                )
            }
        }
        contentView(navigator, mainPane, supportingPane, extraPane)
    }

}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavigationBarLayout(
    modifier: Modifier = Modifier,
    mainPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    supportingPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    extraPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    menuItems: List<Screen> = listOf(),
    navController: NavHostController,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator<Screen>()
    var selectedItem by remember { mutableIntStateOf(0) }

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
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(item)
                        }
                    )
                }
            }
        },
        content = {
            contentView(navigator, mainPane, supportingPane, extraPane)
        }
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun contentView(
    navigator: ThreePaneScaffoldNavigator<Screen>,
    mainPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    supportingPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
    extraPane: @Composable() (ThreePaneScaffoldPaneScope.() -> Unit),
) {
    SupportingPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        mainPane = {
            AnimatedPane {
                mainPane()
            }
        },
        supportingPane = {
            AnimatedPane {
                supportingPane()
            }
        },
        extraPane = {
            AnimatedPane {
                extraPane()
            }
        }
    )
}
