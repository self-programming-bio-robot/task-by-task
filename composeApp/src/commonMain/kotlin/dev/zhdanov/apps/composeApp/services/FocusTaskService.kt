package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing the currently focused task during timer sessions
 */
class FocusTaskService {

    private val _focusedTask = MutableStateFlow<Task?>(null)
    val focusedTask: StateFlow<Task?> = _focusedTask.asStateFlow()

    fun setFocusedTask(task: Task?) {
        _focusedTask.value = task
    }

    fun clearFocusedTask() {
        _focusedTask.value = null
    }

    fun getFocusedTaskId(): Long? = _focusedTask.value?.id
}
