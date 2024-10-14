package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.flow.MutableStateFlow

class TimerSettingsService(
    private val database: Database,
) {
    val isLoading = MutableStateFlow<Boolean>(false)

    val timerSettings = MutableStateFlow<List<TimerSettings>>(emptyList())

    fun loadSettings() {
        isLoading.value = true
        timerSettings.value = database.timerSettingRepository.getAllTimerSettings()
        isLoading.value = false
    }

    fun deleteTimerSetting(id: Long) {
        database.timerSettingRepository.deleteTimerSettingById(id)
        loadSettings()
    }

    fun createTimerSetting(workDuration: Int, shortBreakDuration: Int, longBreakDuration: Int, workCycles: Int) {
        database.timerSettingRepository.insertTimerSetting(
            workDuration = workDuration,
            shortBreakDuration = shortBreakDuration,
            longBreakDuration = longBreakDuration,
            workCycles = workCycles
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
        database.timerSettingRepository.updateTimerSetting(
            id = id,
            workDuration = workDuration,
            shortBreakDuration = shortBreakDuration,
            longBreakDuration = longBreakDuration,
            workCycles = workCycles
        )
        loadSettings()
    }

    fun migration() {
        database.timerSettingRepository.addDefaultTimerSettingIfNotExists(DEFAULT_TIMER_SETTINGS)
        loadSettings()
    }

    fun setDefaultSetting(id: Long) {
        database.timerSettingRepository.setDefaultTimerSetting(id)
        loadSettings()
    }
}
