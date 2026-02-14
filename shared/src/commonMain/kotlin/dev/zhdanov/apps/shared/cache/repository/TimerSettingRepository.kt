package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.cache.timerSettings
import dev.zhdanov.apps.shared.model.TimerSettings

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
        workCycles: Int
    ) {
        database.insertTimerSetting(
            workDuration = workDuration.toLong(),
            shortBreakDuration = shortBreakDuration.toLong(),
            longBreakDuration = longBreakDuration.toLong(),
            workCycles = workCycles.toLong(),
        )
    }

    // Get all TimerSettings (convert Long to Int)
    fun getAllTimerSettings(): List<TimerSettings> {
        return database.selectAllTimerSettings(timerSettings)
            .executeAsList()
    }

    // Get a TimerSetting by id (convert Long to Int)
    fun getTimerSettingById(id: Int): TimerSettings? {
        return database.selectTimerSettingById(id.toLong(), timerSettings)
            .executeAsOneOrNull()
    }

    // Update a TimerSetting by passing the entire TimerSetting object (convert Int to Long)
    fun updateTimerSetting(settings: TimerSettings) {
        updateTimerSetting(
            workDuration = settings.workDuration,
            shortBreakDuration = settings.shortBreakDuration,
            longBreakDuration = settings.longBreakDuration,
            workCycles = settings.workCycles,
            id = settings.id
        )
    }

    // Update TimerSetting by id with individual fields (for reference)
    fun updateTimerSetting(
        id: Long,
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        workCycles: Int
    ) {
        database.updateTimerSetting(
            workDuration = workDuration.toLong(),
            shortBreakDuration = shortBreakDuration.toLong(),
            longBreakDuration = longBreakDuration.toLong(),
            workCycles = workCycles.toLong(),
            id = id
        )
    }

    // Delete a TimerSetting by id
    fun deleteTimerSettingById(id: Long) {
        database.deleteTimerSettingById(id)
    }

    // Set default timer setting
    fun setDefaultTimerSetting(settingId: Long) {
        database.transaction {
            database.unsetDefaultTimerSetting()
            database.setDefaultTimerSettingById(settingId)
        }
    }

    fun addDefaultTimerSettingIfNotExists(settings: TimerSettings) {
        database.transaction {
            database.findDefaultTimerSettings().executeAsOneOrNull() ?: run {
                insertTimerSetting(settings)
                val id = database.lastInsertRowId().executeAsOne()
                setDefaultTimerSetting(id)
            }
        }
    }

    // Get the default timer setting
    fun getDefaultTimerSetting(): TimerSettings? {
        return database.findDefaultTimerSettings(timerSettings)
            .executeAsOneOrNull()
    }
}
