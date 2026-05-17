package dev.zhdanov.apps.composeApp.testing

internal object UiTestTags {
    const val AppRoot = "app-root"
    const val HomeScreen = "home-screen"
    const val TaskListScreen = "task-list-screen"
    const val NewTaskInput = "new-task-input"
    const val AddTaskButton = "add-task-button"

    fun navigationItem(title: String) = "navigation-item-$title"
    fun taskRow(taskId: Long) = "task-row-$taskId"
    fun taskCompletion(taskId: Long) = "task-completion-$taskId"
    fun taskFocus(taskId: Long) = "task-focus-$taskId"
    fun taskToday(taskId: Long) = "task-today-$taskId"
}
