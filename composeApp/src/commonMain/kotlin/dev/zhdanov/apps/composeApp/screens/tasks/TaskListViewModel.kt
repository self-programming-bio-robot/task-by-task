package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskListViewModel(private val database: Database) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks = _tasks.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = database.taskRepository.getAllTasks().sortedWith(
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
                database.taskRepository.addTask(CreateTask(trimmedTitle))
                loadTasks()
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            if (task.isCompleted) {
                // If you want to allow uncompleting tasks, you'll need to add this functionality to your repository
            } else {
                database.taskRepository.completeTask(task.id)
            }
            loadTasks()
        }
    }
}
