package dev.zhdanov.apps.composeApp.components.topBar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.navigation3.runtime.NavKey

/**
 * Registry of top bar actions contributed by the currently composed screens.
 * Each NavEntry may register an actions slot under its own key; the scene
 * level top bar renders the actions of the top-most back stack entry. This
 * lets a screen define its actions inside its own content (with access to
 * its view models, coroutine scope and state) while the bar itself is
 * rendered once at the layout level.
 */
val LocalTopBarActions =
    compositionLocalOf<SnapshotStateMap<NavKey, @Composable RowScope.() -> Unit>> {
        mutableStateMapOf()
    }

/**
 * Registers [actions] for [key] while this composable is in the composition.
 * When [key] is the top-most back stack entry, the scene top bar shows these
 * actions.
 */
@Composable
fun RegisterTopBarActions(
    key: NavKey,
    actions: @Composable RowScope.() -> Unit,
) {
    val registry = LocalTopBarActions.current
    val currentActions by rememberUpdatedState(actions)
    DisposableEffect(registry, key) {
        registry[key] = { currentActions() }
        onDispose { registry.remove(key) }
    }
}
