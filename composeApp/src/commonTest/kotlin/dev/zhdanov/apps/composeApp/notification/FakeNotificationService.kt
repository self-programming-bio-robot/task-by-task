package dev.zhdanov.apps.composeApp.notification

/** Records shown notifications; forms are answered with [NotificationResponse.Unsupported]. */
class FakeNotificationService : NotificationService {

    val shown = mutableListOf<Pair<String, String>>()

    override suspend fun addNotification(text: String, title: String) {
        shown += title to text
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
