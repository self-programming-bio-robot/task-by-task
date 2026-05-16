package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.flow.MutableStateFlow

class TimerSettingsService(
    private val database: Database,
    private val workspaceSessionService: WorkspaceSessionService
) {
    val isLoading = MutableStateFlow<Boolean>(false)

    val timerSettings = MutableStateFlow<List<TimerSettings>>(emptyList())

    fun loadSettings() {
        isLoading.value = true
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        timerSettings.value = database.timerSettingRepository.getAllTimerSettings(
            workspaceSessionService.requireCurrentWorkspaceId()
        )
        isLoading.value = false
    }

    fun deleteTimerSetting(id: Long) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.timerSettingRepository.deleteTimerSettingById(
            id,
            workspaceSessionService.requireCurrentWorkspaceId()
        )
        loadSettings()
    }

    fun createTimerSetting(workDuration: Int, shortBreakDuration: Int, longBreakDuration: Int, workCycles: Int) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.timerSettingRepository.insertTimerSetting(
            workDuration = workDuration,
            shortBreakDuration = shortBreakDuration,
            longBreakDuration = longBreakDuration,
            workCycles = workCycles,
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
        loadSettings()
    }

    fun updateTimerSetting(
        id: Long,
        workDuration: Int,
        shortBreakDuration: Int,
        longBreakDuration: Int,
        workCycles: Int
    ) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.timerSettingRepository.updateTimerSetting(
            id = id,
            workDuration = workDuration,
            shortBreakDuration = shortBreakDuration,
            longBreakDuration = longBreakDuration,
            workCycles = workCycles,
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
        loadSettings()
    }

    fun migration() {
        database.timerSettingRepository.addDefaultTimerSettingIfNotExists(
            DEFAULT_TIMER_SETTINGS,
            workspaceSessionService.requireCurrentWorkspaceId()
        )
        loadSettings()
    }

    fun setDefaultSetting(id: Long) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.timerSettingRepository.setDefaultTimerSetting(
            id,
            workspaceSessionService.requireCurrentWorkspaceId()
        )
        loadSettings()
    }

    // Load the default timer setting from database
    fun loadDefaultTimerSetting(): TimerSettings? {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        return database.timerSettingRepository.getDefaultTimerSetting(
            workspaceSessionService.requireCurrentWorkspaceId()
        )
    }
}
