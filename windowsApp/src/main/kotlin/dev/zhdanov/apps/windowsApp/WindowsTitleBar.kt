package dev.zhdanov.apps.windowsApp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import dev.zhdanov.apps.composeApp.components.window.LocalTitleBarInsets
import dev.zhdanov.apps.composeApp.components.window.TitleBarHeight

/**
 * Merges the native Windows title bar into the app content using the
 * JetBrains Runtime custom title bar: the system caption buttons stay native
 * and are painted over the app's top row, while native drag, double-click
 * maximize, Snap and resize keep working.
 *
 * Falls back to the standard system title bar when the app doesn't run on JBR.
 */
@Composable
internal fun MergedTitleBarWindow(
    window: ComposeWindow,
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val decorations = remember {
        if (JBR.isWindowDecorationsSupported()) JBR.getWindowDecorations() else null
    }
    if (decorations == null) {
        content()
    } else {
        MergedTitleBar(window, decorations, isDarkTheme, content)
    }
}

@Composable
private fun MergedTitleBar(
    window: ComposeWindow,
    decorations: WindowDecorations,
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val titleBar = remember(decorations) {
        decorations.createCustomTitleBar().apply { height = TitleBarHeight.value }
    }
    var insets by remember { mutableStateOf(WindowInsets(0, 0, 0, 0)) }

    LaunchedEffect(window, titleBar, isDarkTheme) {
        titleBar.putProperty("controls.dark", isDarkTheme)
        decorations.setCustomTitleBar(window, titleBar)
        insets = WindowInsets(left = titleBar.leftInset.dp, right = titleBar.rightInset.dp)
    }

    CompositionLocalProvider(LocalTitleBarInsets provides insets) {
        Box(Modifier.fillMaxSize().titleBarHitTest(titleBar)) {
            content()
        }
    }
}

/**
 * Tells JBR, per mouse event, whether the pointer is over an interactive
 * element (client area) or over empty title bar space (native caption).
 * Compose elements that handle input consume the event before it reaches this
 * root handler in the Main pass; anything left unconsumed is draggable. A press
 * that started on a control stays in the client area until release. JBR only
 * honours the result inside the title bar area, so the rest of the window is
 * unaffected.
 */
private fun Modifier.titleBarHitTest(titleBar: WindowDecorations.CustomTitleBar): Modifier =
    pointerInput(titleBar) {
        awaitPointerEventScope {
            var pressedOnControl = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type == PointerEventType.Exit || event.type == PointerEventType.Scroll) continue

                val consumed = event.changes.any { it.isConsumed }
                if (event.type == PointerEventType.Press) pressedOnControl = consumed
                titleBar.forceHitTest(consumed || pressedOnControl)
                if (event.type == PointerEventType.Release) pressedOnControl = false
            }
        }
    }
