package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.withContext

class FocusSessionDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers,
    private val workspaceSessionService: WorkspaceSessionService
) {
    suspend fun addFocusTimeWithTasks(
        duration: Long,
        finishedAt: Long,
        feedback: String?,
        startedAt: Long?,
        pauseTime: Long?,
        taskIds: List<Long>
    ): Long = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.addFocusTimeWithTasks(
            duration = duration,
            finishedAt = finishedAt,
            feedback = feedback?.let(workspaceSessionService::encryptTextForCurrentWorkspace),
            startedAt = startedAt,
            pauseTime = pauseTime,
            taskIds = taskIds,
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    suspend fun getAllFocusTimes(): List<FocusTime> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.getAllFocusTimes(workspaceSessionService.requireCurrentWorkspaceId())
            .map(::decryptFocusTime)
    }

    suspend fun getFocusTimesBetween(from: Long, to: Long): List<FocusTime> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.getAllFocusTimesBetween(
            from = from,
            to = to,
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        ).map(::decryptFocusTime)
    }

    suspend fun getTasksForFocusTime(focusTimeId: Long): List<Task> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.getTasksForFocusTime(focusTimeId, workspaceSessionService.requireCurrentWorkspaceId())
            .map { task ->
                task.copy(
                    title = workspaceSessionService.decryptTextForCurrentWorkspace(task.title),
                    description = workspaceSessionService.decryptNullableTextForCurrentWorkspace(task.description)
                )
            }
    }

    private fun decryptFocusTime(focusTime: FocusTime): FocusTime {
        return focusTime.copy(
            feedback = workspaceSessionService.decryptTextForCurrentWorkspace(focusTime.feedback)
        )
    }
}
