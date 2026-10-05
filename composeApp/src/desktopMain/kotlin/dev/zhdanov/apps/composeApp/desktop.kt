package dev.zhdanov.apps.composeApp

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.*
import dev.zhdanov.apps.composeApp.di.initializeKoin
import dev.zhdanov.apps.composeApp.notification.DesktopNotificationSender
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
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

    Window(
        onCloseRequest = ::exitApplication,
        title = "task-by-task",
    ) {
        App()
    }
}
