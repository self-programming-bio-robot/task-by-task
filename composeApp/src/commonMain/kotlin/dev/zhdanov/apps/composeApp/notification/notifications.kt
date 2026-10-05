package dev.zhdanov.apps.composeApp.notification

data class Notification(
    val text: String,
    val title: String = "task-by-task"
)