package dev.zhdanov.apps.composeApp.notification

/**
 * Shows notifications to the user and reports what they did with them.
 * Implementations are platform-specific and bound in the platform Koin module
 * (on desktop — in each OS application module).
 */
interface NotificationService {

    suspend fun addNotification(text: String, title: String = DEFAULT_NOTIFICATION_TITLE)

    /**
     * Shows a notification with the inputs described by [form].
     * [onResult] is called exactly once — possibly right away (e.g. with
     * [NotificationResponse.Unsupported]) or much later from another coroutine.
     */
    suspend fun <R> addNotification(
        text: String,
        title: String = DEFAULT_NOTIFICATION_TITLE,
        form: NotificationForm<R>,
        submitLabel: String = DEFAULT_SUBMIT_LABEL,
        onResult: suspend (NotificationResponse<R>) -> Unit,
    )
}
