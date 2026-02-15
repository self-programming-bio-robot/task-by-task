package dev.zhdanov.apps.composeApp.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.repository.TaskRepository
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTimeWithTask
import dev.zhdanov.apps.shared.model.FocusTimeWithTasks
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

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
            // Get tasks from junction table (many-to-many)
            val tasksFromJunction = database.getTasksForFocusTime(focusTime.id)
            val primaryTask = tasksFromJunction.firstOrNull()
                ?: focusTime.taskId?.let { tasksMap[it] }

            FocusTimeWithTask(
                focusTime = focusTime,
                task = primaryTask
            )
        }
    }

    @ExperimentalTime
    fun getFocusTimesWithTasksForDate(date: LocalDate): List<FocusTimeWithTask> {
        val timeZone = TimeZone.currentSystemDefault()
        val startOfDay = LocalDateTime(date, LocalTime(0, 0))
            .toInstant(timeZone).toEpochMilliseconds()
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000) // +1 day in milliseconds

        val focusTimes = database.getAllFocusTimesBetween(startOfDay, endOfDay)
        val tasksMap = _tasks.value
        return focusTimes.map { focusTime ->
            // Get tasks from junction table (many-to-many)
            val tasksFromJunction = database.getTasksForFocusTime(focusTime.id)
            val primaryTask = tasksFromJunction.firstOrNull()
                ?: focusTime.taskId?.let { tasksMap[it] }

            FocusTimeWithTask(
                focusTime = focusTime,
                task = primaryTask
            )
        }
    }
}

@Serializable
data class AssistantReviewResponse(
    val date: kotlinx.datetime.LocalDate,
    val summary: String,
    val response: String,
)
