package dev.zhdanov.apps.shared.cache

import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.DaySummaryRecord
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.model.TaskSummary
import dev.zhdanov.apps.shared.model.TimerSettings
import dev.zhdanov.apps.shared.model.Workspace
import dev.zhdanov.apps.shared.model.WorkspaceSecuritySettings
import dev.zhdanov.apps.shared.utils.toLocalDate
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

private val summaryJson = Json { ignoreUnknownKeys = true }

val workspaceMapper = { id: Long, syncId: String, name: String, icon: String, isSelected: Boolean, createdAt: Long, updatedAt: Long, deletedAt: Long? ->
    Workspace(
        id = id,
        syncId = syncId,
        name = name,
        icon = icon,
        isSelected = isSelected,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

val workspaceSecuritySettingsMapper = {
    workspaceId: Long,
    openAiToken: String,
    llmBaseUrl: String,
    llmModelId: String,
    encryptionEnabled: Boolean,
    encryptionSalt: String?,
    wrappedDataKey: String?,
    encryptionIterations: Long ->
    WorkspaceSecuritySettings(
        workspaceId = workspaceId,
        openAiToken = openAiToken,
        llmBaseUrl = llmBaseUrl,
        llmModelId = llmModelId,
        encryptionEnabled = encryptionEnabled,
        encryptionSalt = encryptionSalt,
        wrappedDataKey = wrappedDataKey,
        encryptionIterations = encryptionIterations.toInt()
    )
}

val daySummaryMapper = { date: Long, focusTime: Long, review: String, linkedTasks: String,
                         workspaceId: Long, syncId: String, updatedAt: Long, deletedAt: Long? ->
    DaySummary(
        date = date.toLocalDate(),
        focusTime = focusTime,
        review = review,
        linkedTasks = runCatching {
            summaryJson.decodeFromString<List<TaskSummary>>(linkedTasks)
        }.getOrDefault(emptyList()),
        workspaceId = workspaceId,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

val daySummaryRecordMapper = { date: Long, focusTime: Long, review: String, linkedTasks: String,
                               workspaceId: Long, syncId: String, updatedAt: Long, deletedAt: Long? ->
    DaySummaryRecord(
        date = date.toLocalDate(),
        focusTime = focusTime,
        review = review,
        linkedTasks = linkedTasks,
        workspaceId = workspaceId,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

val focusTimeMapper = { id: Long, duration: Long, feedback: String?, finishedAt: Long, startedAt: Long?, pauseTime: Long?, taskId: Long?,
                        workspaceId: Long, syncId: String, updatedAt: Long, deletedAt: Long? ->
    FocusTime(
        id = id,
        duration = duration.toInt(),
        feedback = feedback ?: "",
        finishedAt = finishedAt,
        startedAt = startedAt,
        pauseTime = pauseTime?.toInt(),
        taskId = taskId,
        workspaceId = workspaceId,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

val timerSettings = { id: Long, workDuration: Long, shortBreakDuration: Long, longBreakDuration: Long, workCycles: Long,
                      isDefault: Boolean, workspaceId: Long, syncId: String, updatedAt: Long, deletedAt: Long? ->
    TimerSettings(
        id = id,
        default = isDefault,
        workDuration = workDuration.toInt(),
        shortBreakDuration = shortBreakDuration.toInt(),
        longBreakDuration = longBreakDuration.toInt(),
        workCycles = workCycles.toInt(),
        workspaceId = workspaceId,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}

@OptIn(ExperimentalTime::class)
val taskMapper = { id: Long, title: String, description: String?, createdAt: Long, completedAt: Long?,
                   isCompleted: Boolean, isToday: Boolean, workspaceId: Long, syncId: String, updatedAt: Long, deletedAt: Long? ->
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
        workspaceId = workspaceId,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}
