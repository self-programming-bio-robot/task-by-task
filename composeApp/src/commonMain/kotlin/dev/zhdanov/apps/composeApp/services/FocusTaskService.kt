package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing selected tasks during timer sessions.
 * Supports multiple tasks per focus session (many-to-many relationship).
 */
class FocusTaskService {

    private val _selectedTasks = MutableStateFlow<List<Task>>(emptyList())
    val selectedTasks: StateFlow<List<Task>> = _selectedTasks.asStateFlow()

    // For backward compatibility with single task selection
    val focusedTask: StateFlow<Task?> = MutableStateFlow(null).also { flow ->
        // This will be deprecated, but kept for compatibility
    }

    /**
     * Toggle a task in the selection.
     * If task is already selected, it will be removed.
     * If task is not selected, it will be added.
     */
    fun toggleTaskSelection(task: Task) {
        val currentTasks = _selectedTasks.value.toMutableList()
        val existingIndex = currentTasks.indexOfFirst { it.id == task.id }

        if (existingIndex >= 0) {
            currentTasks.removeAt(existingIndex)
        } else {
            currentTasks.add(task)
        }

        _selectedTasks.value = currentTasks
    }

    /**
     * Check if a task is currently selected
     */
    fun isTaskSelected(taskId: Long): Boolean {
        return _selectedTasks.value.any { it.id == taskId }
    }

    /**
     * Set a single focused task (replaces all selected tasks)
     */
    fun setFocusedTask(task: Task?) {
        _selectedTasks.value = if (task != null) listOf(task) else emptyList()
    }

    /**
     * Clear all selected tasks
     */
    fun clearSelectedTasks() {
        _selectedTasks.value = emptyList()
    }

    /**
     * Get all selected task IDs
     */
    fun getSelectedTaskIds(): List<Long> {
        return _selectedTasks.value.map { it.id }
    }

    // Backward compatibility
    fun clearFocusedTask() {
        clearSelectedTasks()
    }

    fun getFocusedTaskId(): Long? = _selectedTasks.value.firstOrNull()?.id
}
