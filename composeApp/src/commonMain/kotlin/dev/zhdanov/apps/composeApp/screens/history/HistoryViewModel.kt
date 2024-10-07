package dev.zhdanov.apps.composeApp.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.DaySummary
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class HistoryViewModel(
    private val database: Database
): ViewModel() {

    private val _isLoading = MutableStateFlow<Boolean>(false)
    private val _history = MutableStateFlow<List<DaySummary>>(emptyList())

    val isLoading = _isLoading
        .onStart {
            _isLoading.value = true
            _history.value = database.getAllDaySummaries()
            _isLoading.value = false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

    val history: Flow<List<DaySummary>> = _history.asStateFlow()
}

@Serializable
data class AssistantReviewResponse(
    val summary: String,
    val response: String,
)
