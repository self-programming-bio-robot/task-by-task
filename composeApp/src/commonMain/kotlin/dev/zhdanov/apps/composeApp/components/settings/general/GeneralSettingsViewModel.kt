package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.SettingKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(
    private val database: Database,
) : ViewModel() {
    private val _openAiToken = MutableStateFlow("")
    private val _theme = MutableStateFlow("auto") // auto, light, dark

    val openAiToken = _openAiToken.asStateFlow()
    val theme = _theme.asStateFlow()

    init {
        loadToken()
        loadTheme()
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
}
