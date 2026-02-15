package dev.zhdanov.apps.composeApp.services

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage as OpenAIChatMessage
import com.aallam.openai.api.chat.ChatRole as OpenAIChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dev.zhdanov.apps.composeApp.notification.Notification
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.shared.model.ChatMessage
import dev.zhdanov.apps.shared.model.ChatRole
import dev.zhdanov.apps.shared.model.ChatSession
import dev.zhdanov.apps.shared.model.SettingKey
import dev.zhdanov.apps.shared.cache.Database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatService(
    private val database: Database,
    private val notificationService: NotificationService
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
        if (session == null) {
            return Result.failure(IllegalStateException("No active chat session"))
        }

        val token = database.settingRepository.getSetting<String>(SettingKey.OPENAI_TOKEN)
        if (token == null) {
            _error.value = "OpenAI token not found"
            return Result.failure(IllegalStateException("OpenAI token not found"))
        }

        _isLoading.value = true
        _error.value = null

        return try {
            val openai = OpenAI(token)

            // Create user message
            val userMsg = ChatMessage(
                id = Uuid.random().toString(),
                role = ChatRole.USER,
                content = userMessage,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            // Update session with user message
            val updatedMessages = session.messages + userMsg
            _currentSession.value = session.copy(messages = updatedMessages)

            // Build OpenAI messages
            val openAIMessages = buildOpenAIMessages(session.daySummary, updatedMessages)

            val request = ChatCompletionRequest(
                model = ModelId("gpt-4.1"),
                messages = openAIMessages
            )

            val response = openai.chatCompletion(request)
            val assistantContent = response.choices.firstOrNull()?.message?.content ?: ""

            // Create assistant message
            val assistantMsg = ChatMessage(
                id = Uuid.random().toString(),
                role = ChatRole.ASSISTANT,
                content = assistantContent,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            // Update session with assistant message
            _currentSession.value = _currentSession.value?.copy(
                messages = updatedMessages + assistantMsg
            )

            // Send notification for new buddy message
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

    private fun buildOpenAIMessages(daySummary: String, messages: List<ChatMessage>): List<OpenAIChatMessage> {
        return buildList {
            // System prompt with day context
            add(OpenAIChatMessage(
                role = OpenAIChatRole.System,
                content = buildSystemPrompt(daySummary)
            ))

            // Add conversation history
            messages.forEach { msg ->
                add(OpenAIChatMessage(
                    role = when (msg.role) {
                        ChatRole.USER -> OpenAIChatRole.User
                        ChatRole.ASSISTANT -> OpenAIChatRole.Assistant
                    },
                    content = msg.content
                ))
            }
        }
    }

    private fun buildSystemPrompt(daySummary: String): String {
        return """
            You are a friendly and supportive productivity buddy. You help users reflect on their day and provide encouragement.

            Context about the user's day:
            $daySummary

            Guidelines:
            - Be warm, supportive, and conversational
            - Ask thoughtful follow-up questions
            - Celebrate achievements, no matter how small
            - Gently suggest improvements when appropriate
            - Keep responses concise (2-3 sentences max)
            - Use a friendly, casual tone
        """.trimIndent()
    }
}
