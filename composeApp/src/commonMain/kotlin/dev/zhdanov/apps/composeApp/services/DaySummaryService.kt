package dev.zhdanov.apps.composeApp.services

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.shared.START_OF_DAY
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.SettingKey
import dev.zhdanov.apps.shared.prompts.REVIEW_DAY_PROMPT
import dev.zhdanov.apps.shared.utils.startOfDayWithShift
import dev.zhdanov.apps.shared.utils.toDuration
import dev.zhdanov.apps.shared.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import org.lighthousegames.logging.logging
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DaySummaryService(
    private val database: Database,
    private val schedulerService: SchedulerService,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        schedulerService.addScheduler(
            "Finish day",
            "${START_OF_DAY.minute} ${START_OF_DAY.hour} * * *",
            TimeZone.currentSystemDefault()
        ) {
            coroutineScope.launch {
                val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                if (database.getDaySummary(currentDate) == null) {
                    logger.i { "Finish day at ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}" }
                    finishDay()
                } else {
                    logger.i { "Skip finishing day at ${Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())}" }
                }

                database.taskRepository.cleanTodayTaskList()
            }
        }
    }

    suspend fun finishDay(): AssistantReviewResponse {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val fiveAmToday = LocalDateTime(currentDate.date, START_OF_DAY)

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

        logger.d { review }
        return review
    }

    fun migration() {
        val startOfToday = startOfDayWithShift(Clock.System.now(), shift = START_OF_DAY.toDuration())
        database.getAllFocusTimesBetween(0L, startOfToday.toEpochMilliseconds())
            .groupBy {
                Instant
                    .fromEpochMilliseconds(it.finishedAt)
                    .minus(START_OF_DAY.toDuration())
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }
            .forEach { group ->
                coroutineScope.launch {
                    database.getDaySummary(group.key) ?: run {
                        try {
                            database.addDaySummary(
                                DaySummary(
                                    date = group.key,
                                    focusTime = group.value.sumOf { it.duration }.toLong(),
                                    review = reviewDay(group.value).summary
                                )
                            )
                        } catch (e: Exception) {
                            logger.e(e) { "Failed to add day summary" }
                        }
                    }
                }
            }
    }

    private suspend fun reviewDay(focusTimes: List<FocusTime>): AssistantReviewResponse {
        val token = database.settingRepository.getSetting<String>(SettingKey.OPENAI_TOKEN)
            ?: run { throw IllegalStateException("OpenAI token not found") }

        val openai = OpenAI(
            token = token
        )

        val historyOfDay = focusTimes.joinToString(separator = "\n") {
            """Date: ${it.finishedAt.toLocalDateTime()}
                |Duration: ${it.duration.seconds}
                |${it.feedback}
            """.trimMargin()
        }

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-4.1"),
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

    companion object {
        private val logger = logging(DaySummaryService::class.qualifiedName)
    }
}
