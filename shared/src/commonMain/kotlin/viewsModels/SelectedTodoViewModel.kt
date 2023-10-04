package viewsModels

import dev.icerock.moko.mvvm.viewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import models.TodoItem

class SelectedTodoViewModel: ViewModel() {
    private val _uiState = MutableStateFlow<TodoItem?>(null)
    val uiState = _uiState.asStateFlow()

    fun set(todoItem: TodoItem) {
        _uiState.update {
            todoItem
        }
    }

    fun clear() {
        _uiState.update { null }
    }
}