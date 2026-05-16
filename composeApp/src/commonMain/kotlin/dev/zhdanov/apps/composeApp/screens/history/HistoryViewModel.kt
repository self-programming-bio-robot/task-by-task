package dev.zhdanov.apps.composeApp.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.HistoryService
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTimeWithTasks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

class HistoryViewModel(
    private val historyService: HistoryService
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    private val _history = MutableStateFlow<List<DaySummary>>(emptyList())

    val isLoading = _isLoading.asStateFlow()
    val history = _history.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _history.value = historyService.getHistory()
            _isLoading.value = false
        }
    }

    suspend fun getFocusTimesWithTasksForDate(date: LocalDate): List<FocusTimeWithTasks> {
        return historyService.getFocusTimesWithTasksForDate(date)
    }

    suspend fun getDaySummary(date: LocalDate): DaySummary? {
        return historyService.getDaySummary(date)
    }
}

@Serializable
data class AssistantReviewResponse(
    val date: kotlinx.datetime.LocalDate,
    val summary: String,
    val response: String,
)
