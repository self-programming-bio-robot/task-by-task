package dev.zhdanov.apps.composeApp.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
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
        .onStart { checkActiveDay() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1.seconds), false)

    init {
        daySummaryService.finishDayEvents
            .onEach { checkActiveDay() }
            .launchIn(viewModelScope)
    }

    private fun checkActiveDay() {
        val today = Clock.System.now()
            .minus(5.hours)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        database.getDaySummary(today) ?: run {
            _isActive.value = true
        }
    }

    suspend fun finishDay(): AssistantReviewResponse {
        _isActive.value = false
        return daySummaryService.finishDay()
    }
}
