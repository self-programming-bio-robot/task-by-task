package dev.zhdanov.apps.windowsApp

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import dev.zhdanov.apps.composeApp.platform.PlatformUi

/**
 * Windows-specific window UI: the native title bar is merged into the app's
 * top row (see [MergedTitleBarWindow]).
 */
object WindowsPlatformUi : PlatformUi {

    @Composable
    override fun FrameWindowScope.WindowContent(isDarkTheme: Boolean, content: @Composable () -> Unit) {
        MergedTitleBarWindow(window, isDarkTheme, content)
    }
}
