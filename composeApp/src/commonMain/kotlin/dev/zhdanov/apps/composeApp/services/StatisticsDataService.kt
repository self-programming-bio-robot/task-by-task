package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task

class StatisticsDataService(
    private val focusSessionDataService: FocusSessionDataService,
    private val taskDataService: TaskDataService
) {
    suspend fun getFocusTimesBetween(from: Long, to: Long): List<FocusTime> {
        return focusSessionDataService.getFocusTimesBetween(from, to)
    }

    suspend fun getAllTasks(): List<Task> {
        return taskDataService.getAllTasks()
    }
}
