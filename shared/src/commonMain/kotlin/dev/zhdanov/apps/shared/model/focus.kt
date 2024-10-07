package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusTime(
    val id: Long,
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
)

@Serializable
data class CreateFocusTime(
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
)
