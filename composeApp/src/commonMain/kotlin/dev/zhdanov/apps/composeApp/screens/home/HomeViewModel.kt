package dev.zhdanov.apps.composeApp.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.shared.cache.Database
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class HomeViewModel(
    private val database: Database,
    private val daySummaryService: DaySummaryService,
): ViewModel() {
    private val _isActive = MutableStateFlow<Boolean>(false)

    val isActive = _isActive
        .onStart {
            val today = Clock.System.now()
                .minus(5.hours)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            database.getDaySummary(today) ?: run {
                _isActive.value = true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1.seconds), false)

    suspend fun finishDay(): String {
        _isActive.value = false
        return daySummaryService.finishDay()
    }
}
