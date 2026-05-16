package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.shared.DEFAULT_START_OF_DAY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

class GeneralSettingsViewModel(
    private val settingsService: AppSettingsService,
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
            val token = settingsService.getOpenAiToken()
            _openAiToken.value = token ?: ""
        }
    }

    fun loadTheme() {
        viewModelScope.launch {
            val themeValue = settingsService.getTheme()
            _theme.value = themeValue ?: "auto"
        }
    }

    fun loadStartOfDay() {
        viewModelScope.launch {
            _startOfDay.value = settingsService.getStartOfDay()
        }
    }

    fun updateToken(newToken: String) {
        _openAiToken.value = newToken
        viewModelScope.launch {
            settingsService.saveOpenAiToken(newToken)
        }
    }

    fun updateTheme(newTheme: String) {
        _theme.value = newTheme
        viewModelScope.launch {
            settingsService.saveTheme(newTheme)
        }
    }

    fun updateStartOfDay(newTime: LocalTime) {
        _startOfDay.value = newTime
        viewModelScope.launch {
            settingsService.saveStartOfDay(newTime)
            daySummaryService.updateScheduler()
        }
    }
}
