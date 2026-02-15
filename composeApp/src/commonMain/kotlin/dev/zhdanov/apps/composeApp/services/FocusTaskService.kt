package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for managing the currently focused task during timer sessions.
 *
 * Rules:
 * - Only ONE active task at a time
 * - Can switch to a new task only if the current task is completed
 * - All completed tasks during a session are linked to the focus time (many-to-many)
 */
class FocusTaskService {

    // Currently active task (only one at a time)
    private val _focusedTask = MutableStateFlow<Task?>(null)
    val focusedTask: StateFlow<Task?> = _focusedTask.asStateFlow()

    // Tasks that were completed during this session (for display and linking)
    private val _completedTasks = MutableStateFlow<List<Task>>(emptyList())
    val completedTasks: StateFlow<List<Task>> = _completedTasks.asStateFlow()

    /**
     * Select a task for focusing.
     * If a task is already selected and not completed, this will return false.
     *
     * @param task The task to select
     * @param isTimerRunning Whether the timer is currently running
     * @return true if selection succeeded, false if blocked by incomplete current task
     */
    fun selectTask(task: Task, isTimerRunning: Boolean): Boolean {
        val currentTask = _focusedTask.value

        // If timer is running and current task is not completed, block selection
        if (isTimerRunning && currentTask != null && !isTaskCompleted(currentTask.id)) {
            return false
        }

        // If current task exists and is completed, add it to completed list
        if (currentTask != null && isTaskCompleted(currentTask.id)) {
            addCompletedTask(currentTask)
        }

        _focusedTask.value = task
        return true
    }

    /**
     * Toggle task selection.
     * - If same task is selected, deselect it
     * - If different task, try to select (respects completion rules)
     *
     * @param task The task to toggle
     * @param isTimerRunning Whether the timer is currently running
     * @return true if toggle succeeded, false if blocked
     */
    fun toggleTaskSelection(task: Task, isTimerRunning: Boolean): Boolean {
        val currentTask = _focusedTask.value

        // If clicking the same task, deselect it
        if (currentTask?.id == task.id) {
            // Only allow deselect if timer is not running or task is completed
            if (!isTimerRunning || isTaskCompleted(task.id)) {
                if (isTaskCompleted(task.id)) {
                    addCompletedTask(task)
                }
                _focusedTask.value = null
                return true
            }
            return false
        }

        // Try to select a different task
        return selectTask(task, isTimerRunning)
    }

    /**
     * Update the focused task with new data (e.g., when completed via checkbox)
     */
    fun updateFocusedTask(updatedTask: Task) {
        if (_focusedTask.value?.id == updatedTask.id) {
            _focusedTask.value = updatedTask
        }
    }

    /**
     * Check if a task is currently focused
     */
    fun isTaskFocused(taskId: Long): Boolean {
        return _focusedTask.value?.id == taskId
    }

    /**
     * Check if current focused task is completed
     */
    fun isCurrentTaskCompleted(): Boolean {
        return _focusedTask.value?.let { isTaskCompleted(it.id) } ?: true
    }

    /**
     * Set the focused task directly (no validation - for initialization)
     */
    fun setFocusedTask(task: Task?) {
        _focusedTask.value = task
    }

    /**
     * Clear the focused task (when stopping timer without saving)
     */
    fun clearFocusedTask() {
        _focusedTask.value = null
        _completedTasks.value = emptyList()
    }

    /**
     * Get the current focused task ID
     */
    fun getFocusedTaskId(): Long? = _focusedTask.value?.id

    /**
     * Get all task IDs to link to focus time (completed tasks + current task)
     */
    fun getAllTaskIdsForSession(): List<Long> {
        val completedIds = _completedTasks.value.map { it.id }
        val currentId = _focusedTask.value?.let { listOf(it.id) } ?: emptyList()
        return (completedIds + currentId).distinct()
    }

    /**
     * Get all tasks in the session (completed + current active)
     */
    fun getAllTasksForSession(): List<Task> {
        val completed = _completedTasks.value
        val current = _focusedTask.value?.let { listOf(it) } ?: emptyList()
        return completed + current
    }

    /**
     * Reset session after saving focus time
     */
    fun resetSession() {
        _focusedTask.value = null
        _completedTasks.value = emptyList()
    }

    private fun isTaskCompleted(taskId: Long): Boolean {
        // Check if task is in completed list
        if (_completedTasks.value.any { it.id == taskId }) {
            return true
        }
        // Check current focused task's completion status
        return _focusedTask.value?.let { it.id == taskId && it.isCompleted } ?: false
    }

    private fun addCompletedTask(task: Task) {
        val current = _completedTasks.value.toMutableList()
        if (current.none { it.id == task.id }) {
            current.add(task)
            _completedTasks.value = current
        }
    }
}
