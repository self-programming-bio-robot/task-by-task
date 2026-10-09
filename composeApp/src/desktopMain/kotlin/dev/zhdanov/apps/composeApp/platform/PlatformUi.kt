package dev.zhdanov.apps.composeApp.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.FrameWindowScope

/**
 * Platform-specific UI hooks for the desktop window. Implementations live in
 * the per-OS application modules (windowsApp, macosApp, linuxApp) and are
 * provided to the shared UI through [LocalPlatformUi].
 */
interface PlatformUi {

    /** Window decorations drawn by the OS module (e.g. custom title bar). */
    val undecoratedWindow: Boolean get() = false

    val transparentWindow: Boolean get() = false

    /**
     * Wraps the app content inside the window; can inject OS-specific chrome
     * (e.g. merge the native title bar and provide
     * [dev.zhdanov.apps.composeApp.components.window.LocalTitleBarInsets]).
     */
    @Composable
    fun FrameWindowScope.WindowContent(isDarkTheme: Boolean, content: @Composable () -> Unit) {
        content()
    }
}

val LocalPlatformUi = staticCompositionLocalOf<PlatformUi> { object : PlatformUi {} }
