// shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/repository/TaskRepository.kt
package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.cache.taskMapper
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.Task
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TaskRepository(private val database: AppDatabaseQueries) {

    fun addTask(task: CreateTask) {
        database.insertTask(
            title = task.title,
            description = task.description,
            isToday = task.isToday == true,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun completeTask(id: Long) {
        database.updateTaskCompletion(
            isCompleted = true,
            completedAt = Clock.System.now().toEpochMilliseconds(),
            id = id
        )
    }

    fun getAllTasks(): List<Task> {
        return database.selectAllTasks(taskMapper).executeAsList()
    }

    fun getTaskById(id: Long): Task? {
        return database.selectTaskById(id, taskMapper).executeAsOneOrNull()
    }

    fun deleteTask(id: Long) {
        database.deleteTask(id)
    }

    fun updateTask(task: Task) {
        database.updateTask(
            title = task.title,
            description = task.description,
            id = task.id,
            isToday = task.isToday
        )
    }

    fun cleanTodayTaskList() {
        database.cleanTodayTaskList()
    }

    fun selectAllTodayTasks(): List<Task> {
        return database.selectTodayTasks(taskMapper).executeAsList()
    }
}
