package dev.zhdanov.apps.shared.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Routine(
    val id: Long,
    val title: String,
    val description: String? = null,
    val scheduleType: String = "DAILY",
    val isActive: Boolean = true,
    val workspaceId: Long = DEFAULT_WORKSPACE_ID,
    val syncId: String = "",
    val createdAt: LocalDateTime,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
)

@Serializable
data class CreateRoutine(
    val title: String,
    val description: String? = null,
    val scheduleType: String = "DAILY",
    val isActive: Boolean = true,
)
