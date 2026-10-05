package dev.zhdanov.apps.composeApp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.awt.Color as AwtColor
import java.awt.Dimension
import dev.zhdanov.apps.composeApp.di.initializeKoin
import dev.zhdanov.apps.composeApp.notification.DesktopNotificationSender
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import task_by_task.composeapp.generated.resources.Res
import task_by_task.composeapp.generated.resources.compose_multiplatform

fun main() = application {
    initializeKoin()

    val notificationService = koinInject<NotificationService>()
    val trayState = rememberTrayState()
    val coroutineScope = rememberCoroutineScope()
    val notificationSender = remember { DesktopNotificationSender(trayState) }

    DisposableEffect(Unit) {
        notificationSender.initialize()
        onDispose { notificationSender.shutdown() }
    }

    notificationService.notifications
        .onEach {
            notificationSender.send(it.title, it.text)
        }
        .launchIn(coroutineScope)

    Tray(
        state = trayState,
        icon = painterResource(Res.drawable.compose_multiplatform),
        menu = {
            Item(
                "Notification",
                onClick = {
                    coroutineScope.launch {
                        notificationService.addNotification(
                            Notification("hello")
                        )
                    }
                }
            )
        }
    )

    val appSettingsService = koinInject<AppSettingsService>()
    val themeSetting by appSettingsService.theme.collectAsState()
    val isDarkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "task-by-task",
        state = rememberWindowState(size = DpSize(1100.dp, 750.dp)),
    ) {
        window.minimumSize = Dimension(380, 520)
        // Match the OS-level window background to the app theme so dropped
        // frames don't flash the default light Swing background.
        window.background = if (isDarkTheme) AwtColor(0x14, 0x12, 0x18) else AwtColor(0xFE, 0xF7, 0xFF)
        App()
    }
}
