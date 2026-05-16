package dev.zhdanov.apps.composeApp.notification

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

class NotificationService {
    private val _notifications = Channel<Notification>(Channel.BUFFERED)

    val notifications = _notifications.consumeAsFlow()

    suspend fun addNotification(notification: Notification) {
        _notifications.send(notification)
    }
}
