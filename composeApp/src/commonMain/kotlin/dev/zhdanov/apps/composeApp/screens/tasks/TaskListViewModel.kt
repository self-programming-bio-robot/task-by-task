package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.TaskDataService
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val taskDataService: TaskDataService,
    private val daySummaryService: DaySummaryService
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    val tasks = _tasks.asStateFlow()
    val todayTask = _tasks.map { tasks -> tasks.filter { it.isToday } }

    init {
        loadTasks()
        daySummaryService.finishDayEvents
            .onEach { loadTasks() }
            .launchIn(viewModelScope)
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = taskDataService.getAllTasks().sortedWith(
                compareBy(
                    { it.isCompleted },
                    { it.createdAt }
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

    fun updateTask(updatedTask: Task) {
        viewModelScope.launch {
            taskDataService.updateTask(updatedTask)
            loadTasks()
        }
    }
}
