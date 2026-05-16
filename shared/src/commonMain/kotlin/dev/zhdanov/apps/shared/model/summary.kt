package dev.zhdanov.apps.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DaySummary(
    val date: LocalDate,
    val focusTime: Long,
    val review: String,
    val linkedTasks: List<TaskSummary> = emptyList(),
    val workspaceId: Long = DEFAULT_WORKSPACE_ID,
    val syncId: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

@Serializable
data class TaskSummary(
    val taskId: Long,
    val title: String,
    val totalDuration: Long
)

data class DaySummaryRecord(
    val date: LocalDate,
    val focusTime: Long,
    val review: String,
    val linkedTasks: String,
    val workspaceId: Long = DEFAULT_WORKSPACE_ID,
    val syncId: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
