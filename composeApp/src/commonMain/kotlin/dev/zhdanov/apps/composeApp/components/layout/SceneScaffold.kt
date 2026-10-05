package dev.zhdanov.apps.composeApp.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import dev.zhdanov.apps.composeApp.components.topBar.LocalTopBarActions
import dev.zhdanov.apps.composeApp.components.topBar.TopBar

/**
 * App-wide snackbar host state provided by the scene scaffold. Screens can
 * show errors via `LocalSnackbarHostState.current.showSnackbar(...)` without
 * owning a Scaffold.
 */
val LocalSnackbarHostState = compositionLocalOf { SnackbarHostState() }

/**
 * Unified chrome for every screen: a scene-level [TopBar] (title with
 * breadcrumbs, back button, per-screen actions) above the content area,
 * plus a shared snackbar host. Screens provide their top bar actions via
 * `RegisterTopBarActions` inside their content.
 */
@Composable
fun SceneScaffold(
    title: String,
    hasBack: Boolean,
    onBack: suspend () -> Unit,
    topKey: NavKey?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val actionsRegistry = remember {
        mutableStateMapOf<NavKey, @Composable RowScope.() -> Unit>()
    }
    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(
        LocalTopBarActions provides actionsRegistry,
        LocalSnackbarHostState provides snackbarHostState,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    title = title,
                    hasBack = hasBack,
                    onBack = onBack,
                    actions = {
                        topKey?.let { actionsRegistry[it]?.invoke(this) }
                    }
                )
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
