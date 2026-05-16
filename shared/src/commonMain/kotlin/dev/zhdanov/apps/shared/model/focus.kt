package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusTime(
    val id: Long,
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
    val startedAt: Long? = null,
    val pauseTime: Int? = null,
    val taskId: Long? = null,
    val workspaceId: Long = DEFAULT_WORKSPACE_ID,
    val syncId: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
)

@Serializable
data class CreateFocusTime(
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
    val startedAt: Long? = null,
    val pauseTime: Int? = null,
    val taskId: Long? = null,
)
