package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusTimer(
    val id: Long,
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
)
