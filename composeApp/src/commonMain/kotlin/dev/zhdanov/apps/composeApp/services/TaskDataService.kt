package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.withContext

class TaskDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers
) {
    suspend fun addTask(title: String, isToday: Boolean = false) = withContext(dispatchers.io) {
        database.taskRepository.addTask(CreateTask(title = title, isToday = isToday))
    }

    suspend fun completeTask(id: Long) = withContext(dispatchers.io) {
        database.taskRepository.completeTask(id)
    }

    suspend fun getAllTasks(): List<Task> = withContext(dispatchers.io) {
        database.taskRepository.getAllTasks()
    }

    suspend fun getTaskById(id: Long): Task? = withContext(dispatchers.io) {
        database.taskRepository.getTaskById(id)
    }

    suspend fun updateTask(task: Task) = withContext(dispatchers.io) {
        database.taskRepository.updateTask(task)
    }

    suspend fun cleanTodayTaskList() = withContext(dispatchers.io) {
        database.taskRepository.cleanTodayTaskList()
    }
}
