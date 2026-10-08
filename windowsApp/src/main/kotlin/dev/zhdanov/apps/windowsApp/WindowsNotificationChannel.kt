package dev.zhdanov.apps.windowsApp

import com.diamondedge.logging.logging
import dev.nucleusframework.notification.windows.ShortcutPolicy
import dev.nucleusframework.notification.windows.WindowsNotificationCenter
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.platform.OsNotificationChannel

/**
 * Native WinRT toasts via Nucleus (AWT tray balloons are dead on Windows 11).
 * Reports failure to the caller so it can fall back to the tray notification.
 */
class WindowsNotificationChannel : OsNotificationChannel {

    @Volatile
    private var ready = false

    override fun initialize() {
        ready = runCatching {
            WindowsNotificationCenter.initialize(
                aumid = AUMID,
                appName = APP_NAME,
                shortcutPolicy = ShortcutPolicy.REQUIRE_CREATE,
            )
        }.onFailure { logger.w(it) { "Windows notification init failed" } }
            .getOrDefault(false)

        if (!ready) {
            logger.w { "Windows notifications unavailable, falling back to tray" }
        }
    }

    override fun send(notification: Notification): Boolean {
        if (!ready) return false

        return runCatching {
            WindowsNotificationCenter
                .showSimple(title = notification.title, body = notification.text) { error ->
                    error?.let { logger.w { "Windows toast failed: $it" } }
                }
        }.isSuccess
    }

    override fun shutdown() {
        if (ready) {
            runCatching { WindowsNotificationCenter.uninitialize() }
            ready = false
        }
    }

    companion object {
        private const val AUMID = "dev.zhdanov.TaskByTask"
        private const val APP_NAME = "TaskByTask"
        private val logger = logging(WindowsNotificationChannel::class.qualifiedName)
    }
}
