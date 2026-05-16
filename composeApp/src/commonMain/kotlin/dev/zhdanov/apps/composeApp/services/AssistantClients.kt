package dev.zhdanov.apps.composeApp.services

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage as OpenAIChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole as OpenAIChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import dev.zhdanov.apps.shared.model.ChatMessage
import dev.zhdanov.apps.shared.model.ChatRole
import dev.zhdanov.apps.shared.model.AssistantConfig
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_MODEL
import dev.zhdanov.apps.shared.prompts.REVIEW_DAY_PROMPT
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class DayReviewResult(
    val summary: String,
    val response: String
)

interface ReviewClient {
    suspend fun reviewDay(config: AssistantConfig, historyOfDay: String): DayReviewResult
}

interface ChatClient {
    suspend fun sendMessage(config: AssistantConfig, daySummary: String, messages: List<ChatMessage>): String
}

class OpenAIReviewClient(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ReviewClient {
    override suspend fun reviewDay(config: AssistantConfig, historyOfDay: String): DayReviewResult {
        val result = createOpenAI(config).chatCompletion(
            ChatCompletionRequest(
                model = ModelId(config.modelId.ifBlank { DEFAULT_ASSISTANT_MODEL }),
                responseFormat = ChatResponseFormat.JsonObject,
                messages = listOf(
                    OpenAIChatMessage(
                        role = OpenAIChatRole.System,
                        content = REVIEW_DAY_PROMPT
                    ),
                    OpenAIChatMessage(
                        role = OpenAIChatRole.User,
                        content = historyOfDay
                    )
                )
            )
        )

        val content = result.choices.joinToString {
            it.message.content ?: ""
        }
        val response = json.decodeFromString<OpenAIReviewResponse>(content)
        return DayReviewResult(response.summary, response.response)
    }
}

class OpenAIChatClient : ChatClient {
    override suspend fun sendMessage(config: AssistantConfig, daySummary: String, messages: List<ChatMessage>): String {
        val openAIMessages = buildList {
            add(
                OpenAIChatMessage(
                    role = OpenAIChatRole.System,
                    content = buildSystemPrompt(daySummary)
                )
            )

            messages.forEach { msg ->
                add(
                    OpenAIChatMessage(
                        role = when (msg.role) {
                            ChatRole.USER -> OpenAIChatRole.User
                            ChatRole.ASSISTANT -> OpenAIChatRole.Assistant
                        },
                        content = msg.content
                    )
                )
            }
        }

        val response = createOpenAI(config).chatCompletion(
            ChatCompletionRequest(
                model = ModelId(config.modelId.ifBlank { DEFAULT_ASSISTANT_MODEL }),
                messages = openAIMessages
            )
        )

        return response.choices.firstOrNull()?.message?.content.orEmpty()
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

@Serializable
private data class OpenAIReviewResponse(
    val summary: String,
    val response: String
)

private fun createOpenAI(config: AssistantConfig): OpenAI {
    return OpenAI(
        OpenAIConfig(
            token = config.token,
            host = OpenAIHost(config.baseUrl)
        )
    )
}
