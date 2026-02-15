package dev.zhdanov.apps.composeApp.services

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.diamondedge.logging.logging
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.shared.START_OF_DAY
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class DaySummaryService(
    private val database: Database,
    private val schedulerService: SchedulerService,
    private val taskRepository: TaskRepository,
) {
    val finishDayEvents = MutableSharedFlow<Unit>(0)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        schedulerService.addScheduler(
            "Finish day",
            "${START_OF_DAY.minute} ${START_OF_DAY.hour} * * *",
            TimeZone.currentSystemDefault()
        ) {  plannedTime, actualTime, timeZone ->
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
        val dayDate = currentDateTime.minus(START_OF_DAY.toDuration())
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (database.getDaySummary(dayDate) != null) {
            logger.i { "Day summary already exists for $dayDate" }
            throw IllegalStateException("Day summary already exists for $dayDate")
        }

        val startDateTime = LocalDateTime(dayDate, START_OF_DAY)
        val endDateTime = LocalDateTime(dayDate.plus(1, DateTimeUnit.DAY), START_OF_DAY)

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
        return review
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
       private val logger = logging()
    }
}
