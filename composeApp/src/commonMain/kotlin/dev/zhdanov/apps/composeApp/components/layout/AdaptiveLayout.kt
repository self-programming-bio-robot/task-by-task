package dev.zhdanov.apps.composeApp.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dev.zhdanov.apps.composeApp.components.window.TitleBarHeight
import dev.zhdanov.apps.composeApp.components.workspace.WorkspaceSelector
import dev.zhdanov.apps.composeApp.navigation.MainNavGraph
import dev.zhdanov.apps.composeApp.navigation.NavigationViewModel
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import org.koin.compose.koinInject

val menuItems: List<Screen> = listOf(
    Screen.Home,
    Screen.Statistics,
    Screen.History,
    Screen.TaskList(),
    Screen.Settings
)

/**
 * Coarse app-wide layout tiers derived from the window width:
 * - [Compact]: one content pane, bottom navigation bar
 * - [Medium]: one content pane, side navigation rail
 * - [Expanded]: side navigation rail, up to two content panes
 */
enum class AppLayoutMode { Compact, Medium, Expanded }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberAppLayoutMode(): AppLayoutMode {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        !windowSizeClass.isWidthAtLeastBreakpoint(600) -> AppLayoutMode.Compact
        !windowSizeClass.isWidthAtLeastBreakpoint(840) -> AppLayoutMode.Medium
        else -> AppLayoutMode.Expanded
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveLayout() {
    val layoutMode = rememberAppLayoutMode()
    val viewModel: NavigationViewModel = viewModel { NavigationViewModel() }
    val workspaceSessionService: WorkspaceSessionService = koinInject()
    val currentWorkspace by workspaceSessionService.currentWorkspace.collectAsState()

    when (layoutMode) {
        AppLayoutMode.Compact -> {
            NavigationBarLayout(
                menuItems = menuItems,
                viewModel = viewModel,
            ) {
                key(currentWorkspace?.id) {
                    MainNavGraph(
                        viewModel = viewModel,
                        topBarLeading = { WorkspaceSelector(expandedContent = false) },
                    )
                }
            }
        }

        else -> { // Medium, Expanded
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
        Column(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TitleBarHeight),
                contentAlignment = Alignment.Center
            ) {
                WorkspaceSelector(expandedContent = false)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                menuItems.forEachIndexed { index, item ->
                    RailItem(
                        item = item,
                        selected = selectedIndex == index,
                        onClick = { viewModel.navigateAndClear(item) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RailItem(
    item: Screen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
        tooltip = { PlainTooltip { Text(item.title) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (selected) colors.secondaryContainer else Color.Transparent,
                contentColor = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            ),
            modifier = Modifier
                .testTag(UiTestTags.navigationItem(item.title))
                .size(40.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(20.dp),
            )
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
                        modifier = Modifier.testTag(UiTestTags.navigationItem(item.title)),
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
        val menuScreen = (currentKey as? Screen)?.menuScreen()
        menuItems.indexOfFirst { item -> item::class == menuScreen?.let { it::class } }
            .takeIf { it >= 0 } ?: 0
    }
}
