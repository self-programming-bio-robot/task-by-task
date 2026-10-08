package dev.zhdanov.apps.composeApp.notification

/** Records shown notifications; answers to forms are sent later via [respond]. */
class FakeNotificationService : NotificationService {

    val shown = mutableListOf<Pair<String, String>>()

    /** Callbacks of notifications with forms, in the order they were shown. */
    val pendingForms = mutableListOf<suspend (Map<String, String>?) -> Unit>()

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
        pendingForms += { values ->
            onResult(values?.let { NotificationResponse.Submitted(form.parse(it)) } ?: NotificationResponse.Dismissed)
        }
    }

    /** Answers the form notification [index]: submits [values], or dismisses it when null. */
    suspend fun respond(index: Int, values: Map<String, String>?) = pendingForms[index](values)
}
