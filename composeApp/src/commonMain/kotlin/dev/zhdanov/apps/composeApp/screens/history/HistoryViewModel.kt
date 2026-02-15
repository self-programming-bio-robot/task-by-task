package dev.zhdanov.apps.composeApp.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.repository.TaskRepository
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTimeWithTask
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

class HistoryViewModel(
    private val database: Database,
    private val taskRepository: TaskRepository
): ViewModel() {

    private val _isLoading = MutableStateFlow<Boolean>(false)
    private val _history = MutableStateFlow<List<DaySummary>>(emptyList())
    private val _tasks = MutableStateFlow<Map<Long, Task>>(emptyMap())

    val isLoading = _isLoading
        .onStart {
            _isLoading.value = true
            _history.value = database.getAllDaySummaries()
            _tasks.value = taskRepository.getAllTasks().associateBy { it.id }
            _isLoading.value = false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)

    val history: Flow<List<DaySummary>> = _history.asStateFlow()
    val tasks: Flow<Map<Long, Task>> = _tasks.asStateFlow()

    fun getFocusTimesWithTasks(): List<FocusTimeWithTask> {
        val focusTimes = database.getAllFocusTimes()
        val tasksMap = _tasks.value
        return focusTimes.map { focusTime ->
            FocusTimeWithTask(
                focusTime = focusTime,
                task = focusTime.taskId?.let { taskId ->
                    tasksMap[taskId]
                }
            )
        }
    }
}

@Serializable
data class AssistantReviewResponse(
    val summary: String,
    val response: String,
)
