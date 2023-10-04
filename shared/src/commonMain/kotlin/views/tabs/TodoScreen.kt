package views.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.mvvm.compose.getViewModel
import dev.icerock.moko.mvvm.compose.viewModelFactory
import models.TodoSelectedEvent
import org.koin.compose.getKoin
import services.TodoService
import views.NewTaskInput
import views.TodoList
import viewsModels.SelectedTodoViewModel
import viewsModels.TodoListViewModel

class TodoScreen(
    val onSelected: TodoSelectedEvent? = null
) : Screen {
    @Composable
    override fun Content() {
        val todoService = getKoin().get<TodoService>()
        val todoListViewModel =
            getViewModel(Unit, viewModelFactory { TodoListViewModel(todoService) })
        val navigator = LocalNavigator.currentOrThrow

        Column {
            TodoList(
                todoListViewModel = todoListViewModel,
                modifier = Modifier.weight(1f),
                onSelected = { todo ->
                    onSelected?.let { it(todo) }
                    navigator.pop()
                }
            )
            NewTaskInput {
                todoListViewModel.addTodo(it)
            }
        }
    }
}