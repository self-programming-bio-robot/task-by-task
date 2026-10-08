package dev.zhdanov.apps.composeApp.notification

import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Notification as TrayNotification

/**
 * Notifications via the tray icon — a building block for OS modules without a
 * native backend and a fallback for native ones. The tray can't show inputs,
 * so forms are answered with [NotificationResponse.Unsupported].
 */
class TrayNotificationService(private val trayState: TrayState) : NotificationService {

    override suspend fun addNotification(text: String, title: String) {
        trayState.sendNotification(TrayNotification(title, text))
    }

    override suspend fun <R> addNotification(
        text: String,
        title: String,
        form: NotificationForm<R>,
        submitLabel: String,
        onResult: suspend (NotificationResponse<R>) -> Unit,
    ) {
        addNotification(text, title)
        onResult(NotificationResponse.Unsupported)
    }
}
