package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.RoutineDataService
import dev.zhdanov.apps.composeApp.services.TaskDataService
import dev.zhdanov.apps.shared.model.Routine
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val taskDataService: TaskDataService,
    private val daySummaryService: DaySummaryService,
    private val routineDataService: RoutineDataService
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    val tasks = _tasks.asStateFlow()
    val todayTask = _tasks.map { tasks -> tasks.filter { it.isToday } }

    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines = _routines.asStateFlow()

    init {
        loadTasks()
        loadRoutines()
        merge(
            daySummaryService.finishDayEvents,
            routineDataService.routineEvents
        )
            .onEach {
                loadTasks()
                loadRoutines()
            }
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

    fun loadRoutines() {
        viewModelScope.launch {
            _routines.value = routineDataService.getAllRoutines()
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

    fun addNewRoutine(title: String, description: String? = null) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                routineDataService.addRoutine(trimmedTitle, description)
                loadTasks()
                loadRoutines()
            }
        }
    }

    fun updateRoutine(routine: Routine) {
        viewModelScope.launch {
            routineDataService.updateRoutine(routine)
            loadTasks()
            loadRoutines()
        }
    }

    fun toggleRoutineActive(id: Long) {
        viewModelScope.launch {
            routineDataService.toggleRoutineActive(id)
            loadTasks()
            loadRoutines()
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            routineDataService.deleteRoutine(id)
            loadRoutines()
        }
    }

    fun convertTaskToRoutine(task: Task) {
        viewModelScope.launch {
            routineDataService.convertTaskToRoutine(task)
            loadTasks()
            loadRoutines()
        }
    }
}
