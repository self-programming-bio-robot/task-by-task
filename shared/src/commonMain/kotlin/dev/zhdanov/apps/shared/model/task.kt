package dev.zhdanov.apps.shared.model


import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Long,
    val title: String,
    val description: String? = null,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime? = null,
    val isCompleted: Boolean = false,
    val isToday: Boolean = false,
    val workspaceId: Long = DEFAULT_WORKSPACE_ID,
    val syncId: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
)

@Serializable
data class CreateTask(
    val title: String,
    val description: String? = null,
    val isToday: Boolean? = null
)
