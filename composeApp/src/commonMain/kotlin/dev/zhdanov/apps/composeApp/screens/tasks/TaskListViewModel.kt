package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.RoutineDataService
import dev.zhdanov.apps.composeApp.services.TaskDataService
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val taskDataService: TaskDataService,
    daySummaryService: DaySummaryService,
    private val routineDataService: RoutineDataService
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    val tasks = _tasks.asStateFlow()
    val todayTask = _tasks.map { tasks -> tasks.filter { it.isToday } }

    init {
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
            _tasks.value = taskDataService.getAllTasks().sortedWith(
                compareBy(
                    { it.isCompleted },
                    { !it.isToday },
                )
            )
        }
    }

    fun addNewTask(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                taskDataService.addTask(trimmedTitle)
                loadTasks()
            }
        }
    }

    fun addTodayTask(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                taskDataService.addTask(trimmedTitle, isToday = true)
                loadTasks()
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (task.isCompleted) {
                // If you want to allow uncompleting tasks, you'll need to add this functionality to your repository
            } else {
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
