package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.DEFAULT_START_OF_DAY
import dev.zhdanov.apps.shared.StartOfDaySetting
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.AssistantConfig
import dev.zhdanov.apps.shared.model.SettingKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

class AppSettingsService(
    private val database: Database,
    private val dispatchers: AppDispatchers,
    private val workspaceSessionService: WorkspaceSessionService
) {
    private val _theme = MutableStateFlow("auto")

    val theme = _theme.asStateFlow()

    suspend fun getAssistantConfig(): AssistantConfig? = withContext(dispatchers.io) {
        workspaceSessionService.getAssistantConfig()
    }

    suspend fun getAssistantConfigForUi(): AssistantConfig = withContext(dispatchers.io) {
        workspaceSessionService.getAssistantConfigForUi()
    }

    suspend fun saveAssistantConfig(token: String, baseUrl: String, modelId: String) = withContext(dispatchers.io) {
        workspaceSessionService.saveAssistantConfig(token, baseUrl, modelId)
    }

    suspend fun getTheme(): String? = withContext(dispatchers.io) {
        database.settingRepository.getSetting<String>(SettingKey.THEME)
    }

    suspend fun loadTheme() {
        _theme.value = getTheme() ?: "auto"
    }

    suspend fun saveTheme(theme: String) {
        _theme.value = theme
        withContext(dispatchers.io) {
            database.settingRepository.saveSetting(SettingKey.THEME, theme)
        }
    }

    suspend fun getStartOfDay(): LocalTime = withContext(dispatchers.io) {
        database.settingRepository.getSetting<StartOfDaySetting>(SettingKey.START_OF_DAY)
            ?.toLocalTime()
            ?: DEFAULT_START_OF_DAY
    }

    suspend fun saveStartOfDay(time: LocalTime) = withContext(dispatchers.io) {
        database.settingRepository.saveSetting(
            SettingKey.START_OF_DAY,
            StartOfDaySetting.fromLocalTime(time)
        )
    }
}
