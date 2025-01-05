package dev.zhdanov.apps.shared.cache

import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.model.TimerSettings
import dev.zhdanov.apps.shared.utils.toLocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val daySummaryMapper = { date: Long, focusTime: Long, review: String ->
    DaySummary(
        date = date.toLocalDate(),
        focusTime = focusTime,
        review = review
    )
}

val focusTimeMapper = { id: Long, duration: Long, feedback: String?, finishedAt: Long ->
    FocusTime(
        id = id,
        duration = duration.toInt(),
        feedback = feedback ?: "",
        finishedAt = finishedAt
    )
}

val timerSettings = { id: Long, workDuration: Long, shortBreakDuration: Long, longBreakDuration: Long, workCycles: Long, isDefault: Boolean ->
    TimerSettings(
        id = id,
        default = isDefault,
        workDuration = workDuration.toInt(),
        shortBreakDuration = shortBreakDuration.toInt(),
        longBreakDuration = longBreakDuration.toInt(),
        workCycles = workCycles.toInt()
    )
}

val taskMapper = { id: Long, title: String, description: String?, createdAt: Long, completedAt: Long?, isCompleted: Boolean ->
    Task(
        id = id,
        title = title,
        description = description,
        createdAt = Instant.fromEpochMilliseconds(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()),
        completedAt = completedAt?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()) },
        isCompleted = isCompleted
    )
}
