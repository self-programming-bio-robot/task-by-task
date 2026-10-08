package dev.zhdanov.apps.macosApp

import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel

/**
 * macOS notifications go through the tray icon — the channel reports failure
 * so DesktopNotificationSender falls back to trayState.sendNotification.
 */
class MacOsNotificationChannel : OsNotificationChannel {

    override fun send(notification: Notification): Boolean = false
}
