package dev.zhdanov.apps.shared.cache

import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.model.TaskSummary
import dev.zhdanov.apps.shared.model.TimerSettings
import dev.zhdanov.apps.shared.utils.toLocalDate
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

private val summaryJson = Json { ignoreUnknownKeys = true }

val daySummaryMapper = { date: Long, focusTime: Long, review: String, linkedTasks: String ->
    DaySummary(
        date = date.toLocalDate(),
        focusTime = focusTime,
        review = review,
        linkedTasks = runCatching {
            summaryJson.decodeFromString<List<TaskSummary>>(linkedTasks)
        }.getOrDefault(emptyList())
    )
}

val focusTimeMapper = { id: Long, duration: Long, feedback: String?, finishedAt: Long, startedAt: Long?, pauseTime: Long?, taskId: Long? ->
    FocusTime(
        id = id,
        duration = duration.toInt(),
        feedback = feedback ?: "",
        finishedAt = finishedAt,
        startedAt = startedAt,
        pauseTime = pauseTime?.toInt(),
        taskId = taskId
    )
}

val timerSettings = { id: Long, workDuration: Long, shortBreakDuration: Long, longBreakDuration: Long, workCycles: Long,
                      isDefault: Boolean ->
    TimerSettings(
        id = id,
        default = isDefault,
        workDuration = workDuration.toInt(),
        shortBreakDuration = shortBreakDuration.toInt(),
        longBreakDuration = longBreakDuration.toInt(),
        workCycles = workCycles.toInt()
    )
}

@OptIn(ExperimentalTime::class)
val taskMapper = { id: Long, title: String, description: String?, createdAt: Long, completedAt: Long?,
                   isCompleted: Boolean, isToday: Boolean ->
    Task(
        id = id,
        title = title,
        description = description,
        createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
        completedAt = completedAt?.let {
            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
        },
        isCompleted = isCompleted,
        isToday = isToday,
    )
}
