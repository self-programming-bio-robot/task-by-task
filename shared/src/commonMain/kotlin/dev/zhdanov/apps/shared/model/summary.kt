package dev.zhdanov.apps.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DaySummary(
    val date: LocalDate,
    val focusTime: Long,
    val review: String,
    val linkedTasks: List<TaskSummary> = emptyList()
)

@Serializable
data class TaskSummary(
    val taskId: Long,
    val title: String,
    val totalDuration: Long
)
