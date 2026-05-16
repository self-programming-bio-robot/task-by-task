package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.cache.timerSettings
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class TimerSettingRepository(
    private val database: AppDatabaseQueries
) {
    // Insert a new TimerSetting (convert Int to Long)
    fun insertTimerSetting(settings: TimerSettings) {
        insertTimerSetting(
            workDuration = settings.workDuration,
            shortBreakDuration = settings.shortBreakDuration,
            longBreakDuration = settings.longBreakDuration,
            workCycles = settings.workCycles
        )
    }

    fun insertTimerSetting(
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        workCycles: Int,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        val now = now()
        database.insertTimerSetting(
            workDuration = workDuration.toLong(),
            shortBreakDuration = shortBreakDuration.toLong(),
            longBreakDuration = longBreakDuration.toLong(),
            workCycles = workCycles.toLong(),
            workspaceId = workspaceId,
            syncId = Uuid.random().toString(),
            updatedAt = now
        )
    }

    // Get all TimerSettings (convert Long to Int)
    fun getAllTimerSettings(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<TimerSettings> {
        return database.selectAllTimerSettings(workspaceId, timerSettings)
            .executeAsList()
    }

    // Get a TimerSetting by id (convert Long to Int)
    fun getTimerSettingById(id: Int, workspaceId: Long = DEFAULT_WORKSPACE_ID): TimerSettings? {
        return database.selectTimerSettingById(workspaceId, id.toLong(), timerSettings)
            .executeAsOneOrNull()
    }

    // Update a TimerSetting by passing the entire TimerSetting object (convert Int to Long)
    fun updateTimerSetting(settings: TimerSettings) {
        updateTimerSetting(
            workDuration = settings.workDuration,
            shortBreakDuration = settings.shortBreakDuration,
            longBreakDuration = settings.longBreakDuration,
            workCycles = settings.workCycles,
            id = settings.id,
            workspaceId = settings.workspaceId
        )
    }

    // Update TimerSetting by id with individual fields (for reference)
    fun updateTimerSetting(
        id: Long,
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        workCycles: Int,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        database.updateTimerSetting(
            workDuration = workDuration.toLong(),
            shortBreakDuration = shortBreakDuration.toLong(),
            longBreakDuration = longBreakDuration.toLong(),
            workCycles = workCycles.toLong(),
            updatedAt = now(),
            workspaceId = workspaceId,
            id = id
        )
    }

    // Delete a TimerSetting by id
    fun deleteTimerSettingById(id: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        val now = now()
        database.deleteTimerSettingById(deletedAt = now, updatedAt = now, workspaceId = workspaceId, id = id)
    }

    // Set default timer setting
    fun setDefaultTimerSetting(settingId: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        val now = now()
        database.transaction {
            database.unsetDefaultTimerSetting(updatedAt = now, workspaceId = workspaceId)
            database.setDefaultTimerSettingById(updatedAt = now, workspaceId = workspaceId, id = settingId)
        }
    }

    fun addDefaultTimerSettingIfNotExists(settings: TimerSettings, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        database.transaction {
            database.findDefaultTimerSettings(workspaceId).executeAsOneOrNull() ?: run {
                insertTimerSetting(
                    workDuration = settings.workDuration,
                    shortBreakDuration = settings.shortBreakDuration,
                    longBreakDuration = settings.longBreakDuration,
                    workCycles = settings.workCycles,
                    workspaceId = workspaceId
                )
                val id = database.lastInsertRowId().executeAsOne()
                setDefaultTimerSetting(id, workspaceId)
            }
        }
    }

    // Get the default timer setting
    fun getDefaultTimerSetting(workspaceId: Long = DEFAULT_WORKSPACE_ID): TimerSettings? {
        return database.findDefaultTimerSettings(workspaceId, timerSettings)
            .executeAsOneOrNull()
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
