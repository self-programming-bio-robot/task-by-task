package dev.zhdanov.apps.composeApp.screens.tasks


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.AppPane
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.services.TimerSessionService
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskListScreen(
    initialTaskId: Long? = null,
    onNavigateToTimer: () -> Unit = {},
    onTaskClick: (Long) -> Unit = {},
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val tasks by viewModel.tasks.collectAsState()

    // Auto-navigate to initial task if provided
    LaunchedEffect(initialTaskId) {
        initialTaskId?.let(onTaskClick)
    }

    AppPane(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.testTag(UiTestTags.TaskListScreen),
    ) {
        TaskList(
            tasks,
            onTaskClick = { onTaskClick(it.id) },
            onTaskFocused = onNavigateToTimer
        )
    }
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskEditPane(
    taskId: Long,
    onDone: () -> Unit,
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val tasks by viewModel.tasks.collectAsState()
    val task = tasks.find { it.id == taskId }
    val coroutineScope = rememberCoroutineScope()

    AppPane(
        title = "Edit task",
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (task == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found")
            }
        } else {
            key(taskId) {
                TaskDetails(
                    task = task,
                    onUpdatedTask = { updatedTask ->
                        coroutineScope.launch {
                            viewModel.updateTask(updatedTask)
                            onDone()
                        }
                    },
                    onCancel = onDone
                )
            }
        }
    }
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskFocused: () -> Unit = {}
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val focusTaskService: FocusTaskService = koinInject<FocusTaskService>()
    val timerSessionService: TimerSessionService = koinInject<TimerSessionService>()
    val focusedTask by focusTaskService.focusedTask.collectAsState()
    val isTimerRunning by timerSessionService.isRunning.collectAsState()

    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                NewTaskInput(
                    onAddTask = viewModel::addNewTask
                )
            }
            items(tasks, key = { it.id }) { task ->
                val currentTask = task // Capture for lambda
                TaskItem(
                    task = currentTask,
                    isFocused = focusedTask?.id == currentTask.id,
                    onToggleCompletion = {
                        viewModel.toggleTaskCompletion(currentTask)
                        // Update focused task if this is the focused one
                        if (focusedTask?.id == currentTask.id) {
                            focusTaskService.updateFocusedTask(currentTask.copy(isCompleted = !currentTask.isCompleted))
                        }
                    },
                    onAddToday = { viewModel.updateTask(currentTask.copy(isToday = it)) },
                    onFocusToggle = {
                        // Add to today if not already
                        if (!currentTask.isToday) {
                            viewModel.updateTask(currentTask.copy(isToday = true))
                        }
                        // Try to select the task
                        val success =
                            focusTaskService.toggleTaskSelection(currentTask, isTimerRunning)
                        if (success) {
                            onTaskFocused()
                        }
                    },
                    onClick = { onTaskClick(currentTask) },
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    isFocused: Boolean,
    onToggleCompletion: () -> Unit,
    onAddToday: (Boolean) -> Unit,
    onFocusToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.taskRow(task.id))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion() },
            modifier = Modifier.testTag(UiTestTags.taskCompletion(task.id))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { if (!task.isCompleted || isFocused) onFocusToggle() },
            enabled = !task.isCompleted || isFocused,
            modifier = Modifier.testTag(UiTestTags.taskFocus(task.id))
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = if (isFocused) "Unfocus task" else "Focus task",
                tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconToggleButton(
            checked = task.isToday,
            onCheckedChange = { onAddToday(it) },
            modifier = Modifier.testTag(UiTestTags.taskToday(task.id))
        ) {
            Icon(
                imageVector = Icons.Default.Today,
                contentDescription = "Today",
            )
        }
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
            modifier = Modifier.weight(1f).testTag(UiTestTags.NewTaskInput),
            placeholder = { Text("Enter new task") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = {
                val title = value.trim()
                if (title.isNotEmpty()) {
                    onAddTask(title)
                }
                value = ""
            },
            modifier = Modifier.testTag(UiTestTags.AddTaskButton)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add task")
        }
    }
}

@Composable
fun TaskDetails(task: Task, onUpdatedTask: (task: Task) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description.orEmpty()) }
    var isToday by remember { mutableStateOf(task.isToday) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE // Allow multiline input
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Add to Today")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isToday,
                    onCheckedChange = { isToday = it }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    title = task.title
                    description = task.description.orEmpty()
                    isToday = task.isToday

                    onCancel()
                }
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onUpdatedTask(
                        task.copy(
                            title = title,
                            description = description.ifEmpty { null },
                            isToday = isToday
                        )
                    )
                }
            ) {
                Text("Save")
            }
        }
    }
}




