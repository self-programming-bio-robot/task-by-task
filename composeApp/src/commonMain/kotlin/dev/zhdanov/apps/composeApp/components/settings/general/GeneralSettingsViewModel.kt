package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import dev.zhdanov.apps.shared.DEFAULT_START_OF_DAY
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ICON
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime

class GeneralSettingsViewModel(
    private val settingsService: AppSettingsService,
    private val daySummaryService: DaySummaryService,
    private val workspaceSessionService: WorkspaceSessionService,
) : ViewModel() {
    private val _startOfDay = MutableStateFlow(DEFAULT_START_OF_DAY)
    private val _workspaceName = MutableStateFlow("")
    private val _workspaceIcon = MutableStateFlow(DEFAULT_WORKSPACE_ICON)

    val theme = settingsService.theme
    val startOfDay = _startOfDay.asStateFlow()
    val workspaceName = _workspaceName.asStateFlow()
    val workspaceIcon = _workspaceIcon.asStateFlow()

    init {
        workspaceSessionService.reload()
        observeWorkspace()
        loadTheme()
        loadStartOfDay()
    }

    private fun observeWorkspace() {
        viewModelScope.launch {
            workspaceSessionService.currentWorkspace.collectLatest { workspace ->
                _workspaceName.value = workspace?.name.orEmpty()
                _workspaceIcon.value = workspace?.icon ?: DEFAULT_WORKSPACE_ICON
            }
        }
    }

    fun loadTheme() {
        viewModelScope.launch {
            settingsService.loadTheme()
        }
    }

    fun loadStartOfDay() {
        viewModelScope.launch {
            _startOfDay.value = settingsService.getStartOfDay()
        }
    }

    fun updateTheme(newTheme: String) {
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

    fun updateWorkspaceName(newName: String) {
        _workspaceName.value = newName
        if (newName.isBlank()) {
            return
        }
        viewModelScope.launch {
            workspaceSessionService.renameCurrentWorkspace(newName.trim())
        }
    }

    fun updateWorkspaceIcon(newIcon: String) {
        _workspaceIcon.value = newIcon
        viewModelScope.launch {
            workspaceSessionService.updateCurrentWorkspaceIcon(newIcon)
        }
    }

}
