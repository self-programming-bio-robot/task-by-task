package dev.zhdanov.apps.composeApp.notification

import androidx.compose.ui.window.TrayState
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel

/**
 * Sends desktop notifications through the OS-specific [OsNotificationChannel]
 * provided by the application module (windowsApp, macosApp, linuxApp).
 * Falls back to the tray icon notification when the channel can't deliver.
 */
class DesktopNotificationSender(
    private val trayState: TrayState,
    private val channel: OsNotificationChannel
) {

    fun initialize() {
        channel.initialize()
    }

    fun send(notification: Notification) {
        if (channel.send(notification)) return
        trayState.sendNotification(androidx.compose.ui.window.Notification(
            notification.title,
            notification.text
        ))
    }

    fun shutdown() {
        channel.shutdown()
    }
}
