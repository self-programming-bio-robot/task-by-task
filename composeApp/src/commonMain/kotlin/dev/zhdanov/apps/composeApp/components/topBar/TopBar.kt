package dev.zhdanov.apps.composeApp.components.topBar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.window.LocalTitleBarInsets
import dev.zhdanov.apps.composeApp.components.window.TitleBarHeight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    hasBack: Boolean = false,
    onBack: suspend () -> Unit = {},
    titleExtra: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 32.dp) {
        TopAppBar(
            expandedHeight = TitleBarHeight,
            windowInsets = TopAppBarDefaults.windowInsets.union(LocalTitleBarInsets.current),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    titleExtra()
                }
            },
            navigationIcon = {
                if (hasBack) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = {
                            coroutineScope.launch {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            actions = actions
        )
    }
}
