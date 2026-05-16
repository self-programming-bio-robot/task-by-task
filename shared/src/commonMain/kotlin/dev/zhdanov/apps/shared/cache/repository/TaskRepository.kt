// shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/repository/TaskRepository.kt
package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.cache.taskMapper
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.Task
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class TaskRepository(private val database: AppDatabaseQueries) {

    fun addTask(task: CreateTask, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.insertTask(
            title = task.title,
            description = task.description,
            isToday = task.isToday == true,
            createdAt = now,
            workspaceId = workspaceId,
            syncId = Uuid.random().toString(),
            updatedAt = now
        )
    }

    fun completeTask(id: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.updateTaskCompletion(
            isCompleted = true,
            completedAt = now,
            updatedAt = now,
            workspaceId = workspaceId,
            id = id
        )
    }

    fun getAllTasks(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<Task> {
        return database.selectAllTasks(workspaceId, taskMapper).executeAsList()
    }

    fun getTaskById(id: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID): Task? {
        return database.selectTaskById(workspaceId, id, taskMapper).executeAsOneOrNull()
    }

    fun deleteTask(id: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.deleteTask(deletedAt = now, updatedAt = now, workspaceId = workspaceId, id = id)
    }

    fun updateTask(task: Task, workspaceId: Long = task.workspaceId) {
        database.updateTask(
            title = task.title,
            description = task.description,
            isToday = task.isToday,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId,
            id = task.id
        )
    }

    fun cleanTodayTaskList(workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        database.cleanTodayTaskList(
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId
        )
    }

    fun selectAllTodayTasks(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<Task> {
        return database.selectTodayTasks(workspaceId, taskMapper).executeAsList()
    }
}
