package viewsModels

import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import models.TodoItem
import services.TodoService

data class TodoUIState(
    val list: List<TodoItem> = emptyList(),
)
class TodoListViewModel(
    private val todoService: TodoService,
): ViewModel() {
    private val _uiState = MutableStateFlow(TodoUIState())
    val uiState = _uiState.asStateFlow()

    init {
        updateItems()
    }

    fun updateItems() {
        _uiState.update {
            it.copy(list = todoService.getList().toList())
        }
    }

    fun switchTodo(id: Long, newState: Boolean) {
        todoService.switch(id, newState)
        updateItems()
    }

    fun addTodo(title: String) {
        todoService.add(title)
        updateItems()
    }
}