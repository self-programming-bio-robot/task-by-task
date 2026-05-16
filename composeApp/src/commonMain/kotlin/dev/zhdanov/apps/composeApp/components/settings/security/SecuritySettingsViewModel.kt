package dev.zhdanov.apps.composeApp.components.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import dev.zhdanov.apps.composeApp.services.InvalidWorkspacePinException
import dev.zhdanov.apps.composeApp.services.WorkspaceLockedException
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_BASE_URL
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_MODEL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val settingsService: AppSettingsService,
    private val workspaceSessionService: WorkspaceSessionService,
) : ViewModel() {
    private val _openAiToken = MutableStateFlow("")
    private val _llmBaseUrl = MutableStateFlow(DEFAULT_ASSISTANT_BASE_URL)
    private val _llmModelId = MutableStateFlow(DEFAULT_ASSISTANT_MODEL)
    private val _securityError = MutableStateFlow<String?>(null)

    val openAiToken = _openAiToken.asStateFlow()
    val llmBaseUrl = _llmBaseUrl.asStateFlow()
    val llmModelId = _llmModelId.asStateFlow()
    val securityError = _securityError.asStateFlow()
    val currentWorkspace = workspaceSessionService.currentWorkspace
    val securitySettings = workspaceSessionService.securitySettings
    val isCurrentWorkspaceLocked = workspaceSessionService.isCurrentWorkspaceLocked

    init {
        loadAssistantConfig()
    }

    fun loadAssistantConfig() {
        viewModelScope.launch {
            runCatching {
                settingsService.getAssistantConfigForUi()
            }.onSuccess { config ->
                _openAiToken.value = config.token
                _llmBaseUrl.value = config.baseUrl
                _llmModelId.value = config.modelId
                _securityError.value = null
            }.onFailure { error ->
                if (error is WorkspaceLockedException) {
                    _openAiToken.value = ""
                }
                _securityError.value = error.message
            }
        }
    }

    fun updateToken(newToken: String) {
        _openAiToken.value = newToken
        saveAssistantConfig()
    }

    fun updateBaseUrl(newBaseUrl: String) {
        _llmBaseUrl.value = newBaseUrl
        saveAssistantConfig()
    }

    fun updateModelId(newModelId: String) {
        _llmModelId.value = newModelId
        saveAssistantConfig()
    }

    fun lockWorkspace() {
        workspaceSessionService.lockCurrentWorkspace()
        loadAssistantConfig()
    }

    fun unlockWorkspace(pin: String) {
        runSecurityAction {
            workspaceSessionService.unlockCurrentWorkspace(pin)
            loadAssistantConfig()
        }
    }

    fun enableEncryption(pin: String, confirmation: String) {
        if (pin.isBlank() || pin != confirmation) {
            _securityError.value = "PIN confirmation does not match"
            return
        }
        runSecurityAction {
            workspaceSessionService.enableEncryption(pin)
            loadAssistantConfig()
        }
    }

    fun disableEncryption(pin: String) {
        runSecurityAction {
            workspaceSessionService.disableEncryption(pin)
            loadAssistantConfig()
        }
    }

    private fun saveAssistantConfig() {
        viewModelScope.launch {
            runCatching {
                settingsService.saveAssistantConfig(
                    token = _openAiToken.value,
                    baseUrl = _llmBaseUrl.value,
                    modelId = _llmModelId.value
                )
            }.onFailure { error ->
                _securityError.value = error.message
            }
        }
    }

    private fun runSecurityAction(action: () -> Unit) {
        viewModelScope.launch {
            runCatching(action)
                .onSuccess { _securityError.value = null }
                .onFailure { error ->
                    _securityError.value = when (error) {
                        is InvalidWorkspacePinException -> "Invalid PIN"
                        else -> error.message ?: "Security operation failed"
                    }
                }
        }
    }
}
