package dev.zhdanov.apps.linuxApp

import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel

/**
 * Linux notifications go through the tray icon — the channel reports failure
 * so DesktopNotificationSender falls back to trayState.sendNotification.
 */
class LinuxNotificationChannel : OsNotificationChannel {

    override fun send(notification: Notification): Boolean = false
}
