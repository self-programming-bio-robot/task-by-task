package dev.zhdanov.apps.composeApp.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.zhdanov.apps.composeApp.App
import dev.zhdanov.apps.composeApp.di.initializeKoin
import dev.zhdanov.apps.composeApp.notification.NotificationResponse
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.notification.TextInput
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin
import task_by_task.composeapp.generated.resources.Res
import task_by_task.composeapp.generated.resources.compose_multiplatform
import java.awt.Dimension
import kotlin.system.exitProcess
import java.awt.Color as AwtColor

fun runDesktopApp(osServices: DesktopOsServices) {
    initializeKoin(osServices.koinModule)

    application(exitProcessOnExit = false) {
        Tray(
            state = koinInject<TrayState>(),
            icon = painterResource(Res.drawable.compose_multiplatform),
        )

        val appSettingsService = koinInject<AppSettingsService>()
        val themeSetting by appSettingsService.theme.collectAsState()
        val isDarkTheme = when (themeSetting) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }

        val platformUi = osServices.platformUi

        Window(
            onCloseRequest = ::exitApplication,
            title = "task-by-task",
            state = rememberWindowState(size = DpSize(1100.dp, 750.dp)),
            undecorated = platformUi.undecoratedWindow,
            transparent = platformUi.transparentWindow,
        ) {
            window.minimumSize = Dimension(380, 520)
            // Match the OS-level window background to the app theme so dropped
            // frames don't flash the default light Swing background.
            window.background =
                if (isDarkTheme) AwtColor(0x14, 0x12, 0x18) else AwtColor(0xFE, 0xF7, 0xFF)
            CompositionLocalProvider(LocalPlatformUi provides platformUi) {
                platformUi.WindowContent { App() }
            }
        }
    }

    // Releases OS resources held by singletons (e.g. native notification center).
    stopKoin()
    exitProcess(0)
}
