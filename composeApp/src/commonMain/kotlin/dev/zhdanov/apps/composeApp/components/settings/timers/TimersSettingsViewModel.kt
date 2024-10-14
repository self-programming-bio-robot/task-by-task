package dev.zhdanov.apps.composeApp.components.settings.timers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

class TimersSettingsViewModel(
    private val service: TimerSettingsService
) : ViewModel() {
    val isLoading = service.isLoading
        .onStart {
            service.loadSettings()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1.seconds), false)

    val timerSettings: Flow<List<TimerSettings>> = service.timerSettings.asStateFlow()

    fun removeSetting(id: Long) {
        service.deleteTimerSetting(id)
    }

    fun setDefault(id: Long) {
        service.setDefaultSetting(id)
    }
}
