package dev.zhdanov.apps.composeApp.components.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Height of the app's top row (top bar, rail header). When a platform merges
 * its native title bar into the app content, the merged title bar uses the
 * same height so the top row doubles as the window caption.
 */
val TitleBarHeight = 48.dp

/**
 * Space occupied by native window controls (minimize / maximize / close)
 * painted over the app content when the platform merges its title bar into
 * the app. Only the Windows desktop app provides non-zero insets; everywhere
 * else this stays empty and the UI is unaffected.
 *
 * The top-most row of the UI should apply these insets
 * (`Modifier.windowInsetsPadding`) and consume them for the rest of the tree.
 */
val LocalTitleBarInsets = compositionLocalOf { WindowInsets(0, 0, 0, 0) }
