package dev.zhdanov.apps.composeApp.services

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dev.zhdanov.apps.composeApp.BuildKonfig.OPENAI_KEY
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.prompts.REVIEW_DAY_PROMPT
import dev.zhdanov.apps.shared.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class DaySummaryService(
    private val database: Database,
    private val reviewCache: ReviewCache,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun finishDay(): AssistantReviewResponse {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val fiveAmToday = LocalDateTime(
            year = currentDate.year,
            month = currentDate.month,
            dayOfMonth = currentDate.dayOfMonth,
            hour = 5,
            minute = 0,
            second = 0
        )

        val focusTimes = database.getAllFocusTimesBetween(
            from = fiveAmToday.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            to = currentDate.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )

        val review = reviewDay(focusTimes)
        database.addDaySummary(
            DaySummary(
                date = fiveAmToday.date,
                focusTime = focusTimes.sumOf { it.duration }.toLong(),
                review = review.summary
            )
        )

        return review
    }

    fun migration() {
        database.getAllFocusTimes()
            .groupBy {
                Instant
                    .fromEpochMilliseconds(it.finishedAt)
                    .minus(5.hours)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }
            .forEach { group ->
                coroutineScope.launch {
                    database.getDaySummary(group.key) ?: run {
                        database.addDaySummary(
                            DaySummary(
                                date = group.key,
                                focusTime = group.value.sumOf { it.duration }.toLong(),
                                review = reviewDay(group.value).summary
                            )
                        )
                    }
                }
            }
    }

    private suspend fun reviewDay(focusTimes: List<FocusTime>): AssistantReviewResponse {
        val openai = OpenAI(
            token = OPENAI_KEY
        )

        val historyOfDay = focusTimes.joinToString(separator = "\n") {
            """Date: ${it.finishedAt.toLocalDateTime()}
                |Duration: ${it.duration.seconds}
                |${it.feedback}
            """.trimMargin()
        }

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4o"),
            responseFormat = ChatResponseFormat.JsonObject,
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = REVIEW_DAY_PROMPT
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = historyOfDay
                )
            )
        )

        val result = openai.chatCompletion(chatCompletionRequest)

        val content = result.choices.joinToString {
            it.message.content ?: ""
        }

        return Json.decodeFromString<AssistantReviewResponse>(content)
    }
}
