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

    val openAiToken = _openAiToken.asStateFlow()

    init {
        loadToken()
    }

    fun loadToken() {
        viewModelScope.launch {
            val token = database.settingRepository.getSetting<String>(SettingKey.OPENAI_TOKEN)
            _openAiToken.value = token ?: ""
        }
    }

    fun updateToken(newToken: String) {
        _openAiToken.value = newToken
        viewModelScope.launch {
            database.settingRepository.saveSetting(SettingKey.OPENAI_TOKEN, newToken)
        }
    }
}
