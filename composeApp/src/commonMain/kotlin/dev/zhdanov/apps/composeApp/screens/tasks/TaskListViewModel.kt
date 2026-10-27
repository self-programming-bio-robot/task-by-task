package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.LogicalDayService
import dev.zhdanov.apps.composeApp.services.RoutineDataService
import dev.zhdanov.apps.composeApp.services.TaskDataService
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class TaskListViewModel(
    private val taskDataService: TaskDataService,
    daySummaryService: DaySummaryService,
    private val routineDataService: RoutineDataService,
    private val settingsService: AppSettingsService,
    private val logicalDayService: LogicalDayService,
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    private val _filter = MutableStateFlow(TaskListFilter())
    private val _searchQuery = MutableStateFlow("")

    val tasks = _tasks.asStateFlow()
    val today: StateFlow<LocalDate> = logicalDayService.currentDay
    val filter = _filter.asStateFlow()
    val searchQuery = _searchQuery.asStateFlow()

    val groupedTasks: StateFlow<List<TaskGroup>> = combine(
        _tasks,
        _filter,
        _searchQuery,
        today,
    ) { tasks, filter, query, currentDay ->
        tasks.applyFilter(filter, query, currentDay)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTask: Flow<List<Task>> = combine(_tasks, today) { tasks, currentDay ->
        tasks
            .filter { it.isDueOn(currentDay) }
            .sortedWith(compareBy<Task>({ it.isCompleted }).thenByDescending { it.createdAt })
    }

    init {
        viewModelScope.launch {
            logicalDayService.currentLogicalDay()
            _filter.value = settingsService.getTaskListFilter()
        }
        loadTasks()
        val refreshEvents: Flow<Unit> = merge(
            daySummaryService.finishDayEvents,
            routineDataService.routineEvents
        )
        refreshEvents
            .onEach { loadTasks() }
            .launchIn(viewModelScope)
    }

    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = taskDataService.getAllTasks()
        }
    }

    fun updateFilter(transform: (TaskListFilter) -> TaskListFilter) {
        val updated = transform(_filter.value)
        _filter.value = updated
        viewModelScope.launch {
            settingsService.saveTaskListFilter(updated)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDueToday(task: Task, dueToday: Boolean) {
        viewModelScope.launch {
            val currentDay = logicalDayService.currentLogicalDay()
            val newDate = if (dueToday) currentDay.toLong() else null
            taskDataService.updateTask(task.copy(targetDate = newDate))
            loadTasks()
        }
    }

    fun addNewTask(title: String, targetDate: LocalDate? = null) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                taskDataService.addTask(trimmedTitle, targetDate = targetDate)
                loadTasks()
            }
        }
    }

    fun addTodayTask(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                val currentDay = logicalDayService.currentLogicalDay()
                taskDataService.addTask(trimmedTitle, targetDate = currentDay)
                loadTasks()
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (!task.isCompleted) {
                taskDataService.completeTask(task.id)
            }
            loadTasks()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDataService.updateTask(task)
            loadTasks()
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            taskDataService.deleteTask(id)
            loadTasks()
        }
    }

    fun convertTaskToRoutine(task: Task) {
        viewModelScope.launch {
            routineDataService.convertTaskToRoutine(task)
            loadTasks()
        }
    }
}
