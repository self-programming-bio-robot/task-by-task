package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.DEFAULT_START_OF_DAY
import dev.zhdanov.apps.shared.StartOfDaySetting
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.SettingKey
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

class AppSettingsService(
    private val database: Database,
    private val dispatchers: AppDispatchers
) {
    suspend fun getOpenAiToken(): String? = withContext(dispatchers.io) {
        database.settingRepository.getSetting<String>(SettingKey.OPENAI_TOKEN)
    }

    suspend fun saveOpenAiToken(token: String) = withContext(dispatchers.io) {
        database.settingRepository.saveSetting(SettingKey.OPENAI_TOKEN, token)
    }

    suspend fun getTheme(): String? = withContext(dispatchers.io) {
        database.settingRepository.getSetting<String>(SettingKey.THEME)
    }

    suspend fun saveTheme(theme: String) = withContext(dispatchers.io) {
        database.settingRepository.saveSetting(SettingKey.THEME, theme)
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
