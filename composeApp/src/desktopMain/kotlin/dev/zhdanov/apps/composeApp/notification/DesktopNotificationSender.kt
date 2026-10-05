package dev.zhdanov.apps.composeApp.notification

import androidx.compose.ui.window.TrayState
import com.diamondedge.logging.logging
import dev.nucleusframework.notification.windows.ShortcutPolicy
import dev.nucleusframework.notification.windows.WindowsNotificationCenter

/**
 * Sends desktop notifications. On Windows uses native WinRT toasts via Nucleus
 * (AWT tray balloons are dead on Windows 11); on macOS/Linux falls back to the
 * tray icon notification.
 */
class DesktopNotificationSender(private val trayState: TrayState) {

    private val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    @Volatile
    private var windowsReady = false

    fun initialize() {
        if (!isWindows) return

        windowsReady = runCatching {
            WindowsNotificationCenter.initialize(
                aumid = AUMID,
                appName = APP_NAME,
                shortcutPolicy = ShortcutPolicy.REQUIRE_CREATE,
            )
        }.onFailure { logger.w(it) { "Windows notification init failed" } }
            .getOrDefault(false)

        if (!windowsReady) {
            logger.w { "Windows notifications unavailable, falling back to tray" }
        }
    }

    fun send(title: String, message: String) {
        if (windowsReady && sendWindowsToast(title, message)) return
        trayState.sendNotification(androidx.compose.ui.window.Notification(title, message))
    }

    fun shutdown() {
        if (windowsReady) {
            runCatching { WindowsNotificationCenter.uninitialize() }
            windowsReady = false
        }
    }

    private fun sendWindowsToast(title: String, message: String): Boolean = runCatching {
        WindowsNotificationCenter.showSimple(title = title, body = message) { error ->
            error?.let { logger.w { "Windows toast failed: $it" } }
        }
    }.isSuccess

    companion object {
        private const val AUMID = "dev.zhdanov.TaskByTask"
        private const val APP_NAME = "TaskByTask"
        private val logger = logging(DesktopNotificationSender::class.qualifiedName)
    }
}
