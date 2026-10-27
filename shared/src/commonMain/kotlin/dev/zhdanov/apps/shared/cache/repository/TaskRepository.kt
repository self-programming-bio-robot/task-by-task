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
        if (task.routineId != null) {
            database.insertTaskWithRoutine(
                title = task.title,
                description = task.description,
                routineId = task.routineId,
                targetDate = task.targetDate,
                createdAt = now,
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = now
            )
        } else {
            database.insertTask(
                title = task.title,
                description = task.description,
                targetDate = task.targetDate,
                createdAt = now,
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = now
            )
        }
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
            targetDate = task.targetDate,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId,
            id = task.id
        )
    }

    fun attachTaskToRoutine(
        taskId: Long,
        routineId: Long,
        targetDate: Long,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        database.updateTaskRoutine(
            routineId = routineId,
            targetDate = targetDate,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId,
            id = taskId
        )
    }

    fun getTaskByRoutineAndDate(
        routineId: Long,
        targetDate: Long,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ): Task? {
        return database.selectTaskByRoutineAndDate(workspaceId, routineId, targetDate, taskMapper).executeAsOneOrNull()
    }

    fun getRoutineTasksInPeriod(
        from: Long,
        to: Long,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ): List<Task> {
        return database.selectRoutineTasksInPeriod(workspaceId, from, to, taskMapper).executeAsList()
    }

    fun getCompletedRoutineTasksUpToDate(
        to: Long,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ): List<Task> {
        return database.selectCompletedRoutineTasksUpToDate(workspaceId, to, taskMapper).executeAsList()
    }
}
