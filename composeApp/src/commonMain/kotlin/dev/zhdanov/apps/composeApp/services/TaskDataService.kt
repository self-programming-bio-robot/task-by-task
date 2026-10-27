package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class TaskDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers,
    private val workspaceSessionService: WorkspaceSessionService
) {
    suspend fun addTask(title: String, targetDate: LocalDate? = null) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.addTask(
            CreateTask(
                title = workspaceSessionService.encryptTextForCurrentWorkspace(title),
                targetDate = targetDate?.toLong()
            ),
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    suspend fun completeTask(id: Long) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.completeTask(
            id,
            workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    suspend fun getAllTasks(): List<Task> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.getAllTasks(workspaceSessionService.requireCurrentWorkspaceId())
            .map(::decryptTask)
    }

    suspend fun getTaskById(id: Long): Task? = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.getTaskById(id, workspaceSessionService.requireCurrentWorkspaceId())
            ?.let(::decryptTask)
    }

    suspend fun updateTask(task: Task) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.updateTask(
            task.copy(
                title = workspaceSessionService.encryptTextForCurrentWorkspace(task.title),
                description = workspaceSessionService.encryptNullableTextForCurrentWorkspace(task.description)
            ),
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    suspend fun deleteTask(id: Long) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.deleteTask(
            id = id,
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    suspend fun attachTaskToRoutine(
        taskId: Long,
        routineId: Long,
        targetDate: Long
    ) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.taskRepository.attachTaskToRoutine(
            taskId = taskId,
            routineId = routineId,
            targetDate = targetDate,
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    private fun decryptTask(task: Task): Task {
        return task.copy(
            title = workspaceSessionService.decryptTextForCurrentWorkspace(task.title),
            description = workspaceSessionService.decryptNullableTextForCurrentWorkspace(task.description)
        )
    }
}
