package dev.zhdanov.apps.composeApp.screens.tasks


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.shared.model.Task
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskListScreen() {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val tasks by viewModel.tasks.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onToggleCompletion = { viewModel.toggleTaskCompletion(task) }
                )
            }
            item {
                NewTaskInput(
                    onAddTask = viewModel::addNewTask
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggleCompletion: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion() }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NewTaskInput(
    onAddTask: (value: String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Enter new task") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(onClick = { onAddTask(value) }) {
            Icon(Icons.Default.Add, contentDescription = "Add task")
        }
    }
}
