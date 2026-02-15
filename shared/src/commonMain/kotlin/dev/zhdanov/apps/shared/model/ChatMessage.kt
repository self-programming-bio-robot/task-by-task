package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val timestamp: Long
)

enum class ChatRole {
    USER,
    ASSISTANT
}

@Serializable
data class ChatSession(
    val id: String,
    val date: kotlinx.datetime.LocalDate,
    val messages: List<ChatMessage> = emptyList(),
    val daySummary: String = ""
)
