package dev.zhdanov.apps.composeApp.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeViewModel(
    private val daySummaryService: DaySummaryService,
) : ViewModel() {
    private val _isActive = MutableStateFlow<Boolean>(false)

    val isActive = _isActive
        .onStart { checkActiveDay() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1.seconds), false)

    init {
        daySummaryService.finishDayEvents
            .onEach { checkActiveDay() }
            .launchIn(viewModelScope)
    }

    private suspend fun checkActiveDay() {
        _isActive.value = daySummaryService.isCurrentDayActive()
    }

    suspend fun finishDay(): AssistantReviewResponse {
        val review = daySummaryService.finishDay()
        _isActive.value = false
        return review
    }
}
