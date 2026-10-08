package dev.zhdanov.apps.composeApp.notification

const val DEFAULT_NOTIFICATION_TITLE = "task-by-task"
const val DEFAULT_SUBMIT_LABEL = "OK"

sealed interface NotificationResponse<out R> {

    data class Submitted<out R>(val value: R) : NotificationResponse<R>

    /** The user closed the notification without submitting. */
    data object Dismissed : NotificationResponse<Nothing>

    /** The platform can't show interactive notifications (tray fallback, mobile). */
    data object Unsupported : NotificationResponse<Nothing>
}
