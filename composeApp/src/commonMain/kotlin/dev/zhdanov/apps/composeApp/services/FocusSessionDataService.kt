package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.withContext

class FocusSessionDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers
) {
    suspend fun addFocusTimeWithTasks(
        duration: Long,
        finishedAt: Long,
        feedback: String?,
        startedAt: Long?,
        pauseTime: Long?,
        taskIds: List<Long>
    ): Long = withContext(dispatchers.io) {
        database.addFocusTimeWithTasks(
            duration = duration,
            finishedAt = finishedAt,
            feedback = feedback,
            startedAt = startedAt,
            pauseTime = pauseTime,
            taskIds = taskIds
        )
    }

    suspend fun getAllFocusTimes(): List<FocusTime> = withContext(dispatchers.io) {
        database.getAllFocusTimes()
    }

    suspend fun getFocusTimesBetween(from: Long, to: Long): List<FocusTime> = withContext(dispatchers.io) {
        database.getAllFocusTimesBetween(from, to)
    }

    suspend fun getTasksForFocusTime(focusTimeId: Long): List<Task> = withContext(dispatchers.io) {
        database.getTasksForFocusTime(focusTimeId)
    }
}
