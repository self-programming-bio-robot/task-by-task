package dev.zhdanov.apps.composeApp.platform

import dev.zhdanov.apps.composeApp.notification.Notification

/**
 * OS-specific notification backend. Implementations live in the per-OS
 * application modules (windowsApp, macosApp, linuxApp) and are bound via Koin.
 * Returning false from [send] falls back to the tray icon notification.
 */
interface OsNotificationChannel {

    fun initialize() {}

    fun send(notification: Notification): Boolean

    fun shutdown() {}
}
