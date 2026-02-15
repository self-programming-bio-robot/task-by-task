package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.shared.DEFAULT_START_OF_DAY
import dev.zhdanov.apps.shared.StartOfDaySetting
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.SettingKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

class GeneralSettingsViewModel(
    private val database: Database,
    private val daySummaryService: DaySummaryService,
) : ViewModel() {
    private val _openAiToken = MutableStateFlow("")
    private val _theme = MutableStateFlow("auto") // auto, light, dark
    private val _startOfDay = MutableStateFlow(DEFAULT_START_OF_DAY)

    val openAiToken = _openAiToken.asStateFlow()
    val theme = _theme.asStateFlow()
    val startOfDay = _startOfDay.asStateFlow()

    init {
        loadToken()
        loadTheme()
        loadStartOfDay()
    }

    fun loadToken() {
        viewModelScope.launch {
            val token = database.settingRepository.getSetting<String>(SettingKey.OPENAI_TOKEN)
            _openAiToken.value = token ?: ""
        }
    }

    fun loadTheme() {
        viewModelScope.launch {
            val themeValue = database.settingRepository.getSetting<String>(SettingKey.THEME)
            _theme.value = themeValue ?: "auto"
        }
    }

    fun loadStartOfDay() {
        viewModelScope.launch {
            val setting = database.settingRepository.getSetting<StartOfDaySetting>(SettingKey.START_OF_DAY)
            _startOfDay.value = setting?.toLocalTime() ?: DEFAULT_START_OF_DAY
        }
    }

    fun updateToken(newToken: String) {
        _openAiToken.value = newToken
        viewModelScope.launch {
            database.settingRepository.saveSetting(SettingKey.OPENAI_TOKEN, newToken)
        }
    }

    fun updateTheme(newTheme: String) {
        _theme.value = newTheme
        viewModelScope.launch {
            database.settingRepository.saveSetting(SettingKey.THEME, newTheme)
        }
    }

    fun updateStartOfDay(newTime: LocalTime) {
        _startOfDay.value = newTime
        viewModelScope.launch {
            database.settingRepository.saveSetting(
                SettingKey.START_OF_DAY,
                StartOfDaySetting.fromLocalTime(newTime)
            )
            // Update the scheduler with the new start of day
            daySummaryService.updateScheduler()
        }
    }
}
