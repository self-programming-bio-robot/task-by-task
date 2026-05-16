package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.shared.model.ChatMessage
import dev.zhdanov.apps.shared.model.ChatRole
import dev.zhdanov.apps.shared.model.ChatSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatService(
    private val workspaceSessionService: WorkspaceSessionService,
    private val notificationService: NotificationService,
    private val chatClient: ChatClient
) {
    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun startSession(date: LocalDate, daySummary: String) {
        _currentSession.value = ChatSession(
            id = Uuid.random().toString(),
            date = date,
            daySummary = daySummary
        )
        _error.value = null
    }

    fun clearSession() {
        _currentSession.value = null
        _error.value = null
    }

    suspend fun sendMessage(userMessage: String): Result<ChatMessage> {
        val session = _currentSession.value
            ?: return Result.failure(IllegalStateException("No active chat session"))

        val assistantConfig = workspaceSessionService.getAssistantConfig()
        if (assistantConfig == null) {
            _error.value = "OpenAI token not found"
            return Result.failure(IllegalStateException("OpenAI token not found"))
        }

        _isLoading.value = true
        _error.value = null

        return try {
            val userMsg = ChatMessage(
                id = Uuid.random().toString(),
                role = ChatRole.USER,
                content = userMessage,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            val updatedMessages = session.messages + userMsg
            _currentSession.value = session.copy(messages = updatedMessages)

            val assistantContent = chatClient.sendMessage(assistantConfig, session.daySummary, updatedMessages)
            val assistantMsg = ChatMessage(
                id = Uuid.random().toString(),
                role = ChatRole.ASSISTANT,
                content = assistantContent,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            _currentSession.value = _currentSession.value?.copy(
                messages = updatedMessages + assistantMsg
            )

            notificationService.addNotification(
                Notification("Buddy replied: ${assistantContent.take(50)}${if (assistantContent.length > 50) "..." else ""}")
            )

            Result.success(assistantMsg)
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to send message"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
}
