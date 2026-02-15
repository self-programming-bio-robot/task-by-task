package dev.zhdanov.apps.composeApp.services

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.diamondedge.logging.logging
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.shared.DEFAULT_START_OF_DAY
import dev.zhdanov.apps.shared.StartOfDaySetting
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.repository.TaskRepository
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.SettingKey
import dev.zhdanov.apps.shared.model.TaskSummary
import dev.zhdanov.apps.shared.prompts.REVIEW_DAY_PROMPT
import dev.zhdanov.apps.shared.utils.startOfDayWithShift
import dev.zhdanov.apps.shared.utils.toDuration
import dev.zhdanov.apps.shared.utils.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * Response from OpenAI for day review
 */
@Serializable
private data class OpenAIReviewResponse(
    val summary: String,
    val response: String
)

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class DaySummaryService(
    private val database: Database,
    private val schedulerService: SchedulerService,
    private val taskRepository: TaskRepository,
) {
    val finishDayEvents = MutableSharedFlow<Unit>(0)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Get start of day from settings, or return default if not set.
     */
    private fun getStartOfDay(): LocalTime {
        return database.settingRepository.getSetting<StartOfDaySetting>(SettingKey.START_OF_DAY)
            ?.toLocalTime()
            ?: DEFAULT_START_OF_DAY
    }

    /**
     * Get start of day as Duration from midnight.
     */
    private fun getStartOfDayDuration(): Duration {
        val startOfDay = getStartOfDay()
        return startOfDay.toDuration()
    }

    init {
        // Initial scheduler setup with current start of day setting
        updateScheduler()
    }

    /**
     * Update the scheduler with the current start of day setting.
     * Call this when the start of day setting changes.
     */
    fun updateScheduler() {
        val startOfDay = getStartOfDay()
        schedulerService.addScheduler(
            "Finish day",
            "${startOfDay.minute} ${startOfDay.hour} * * *",
            TimeZone.currentSystemDefault()
        ) { plannedTime, actualTime, timeZone ->
            coroutineScope.launch {
                try {
                    finishDay(plannedTime.minus(1.seconds))
                    logger.i { "Finish day at ${actualTime.toLocalDateTime(timeZone)} for planned time: ${plannedTime.toLocalDateTime(timeZone)}" }
                } catch (e: Exception) {
                    logger.i { "Skip finishing day at ${actualTime.toLocalDateTime(timeZone)} for planned time: ${plannedTime.toLocalDateTime(timeZone)}" }
                }

                database.taskRepository.cleanTodayTaskList()
                finishDayEvents.emit(Unit)
            }
        }
    }

    suspend fun finishDay(currentDateTime: Instant = Clock.System.now()): AssistantReviewResponse {
        val startOfDayDuration = getStartOfDayDuration()
        val dayDate = currentDateTime.minus(startOfDayDuration)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (database.getDaySummary(dayDate) != null) {
            logger.i { "Day summary already exists for $dayDate" }
            throw IllegalStateException("Day summary already exists for $dayDate")
        }

        val startOfDay = getStartOfDay()
        val startDateTime = LocalDateTime(dayDate, startOfDay)
        val endDateTime = LocalDateTime(dayDate.plus(1, DateTimeUnit.DAY), startOfDay)

        val focusTimes = database.getAllFocusTimesBetween(
            from = startDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            to = endDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        )

        // Build linked tasks summary
        val linkedTasks = buildLinkedTasks(focusTimes)

        val review = reviewDay(focusTimes)
        database.addDaySummary(
            DaySummary(
                date = dayDate,
                focusTime = focusTimes.sumOf { it.duration }.toLong(),
                review = review.summary,
                linkedTasks = linkedTasks
            )
        )

        logger.d { review }
        return AssistantReviewResponse(
            date = dayDate,
            summary = review.summary,
            response = review.response
        )
    }

    private fun buildLinkedTasks(focusTimes: List<FocusTime>): List<TaskSummary> {
        val tasksMap = taskRepository.getAllTasks().associateBy { it.id }

        return focusTimes
            .filter { it.taskId != null }
            .groupBy { it.taskId!! }
            .map { (taskId, times) ->
                val task = tasksMap[taskId]
                TaskSummary(
                    taskId = taskId,
                    title = task?.title ?: "Unknown task",
                    totalDuration = times.sumOf { it.duration }.toLong()
                )
            }
            .sortedByDescending { it.totalDuration }
    }

    fun migration() {
        val startOfDayDuration = getStartOfDayDuration()
        val startOfToday = startOfDayWithShift(Clock.System.now(), shift = startOfDayDuration)
        database.getAllFocusTimesBetween(0L, startOfToday.toEpochMilliseconds())
            .groupBy {
                Instant
                    .fromEpochMilliseconds(it.finishedAt)
                    .minus(startOfDayDuration)
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

    private suspend fun reviewDay(focusTimes: List<FocusTime>): OpenAIReviewResponse {
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

        return Json.decodeFromString<OpenAIReviewResponse>(content)
    }

    companion object {
       private val logger = logging()
    }
}
