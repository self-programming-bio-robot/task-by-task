package dev.zhdanov.apps.composeApp.screens.tasks


import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import dev.zhdanov.apps.composeApp.services.FocusTaskService

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    initialTaskId: Long? = null,
    onNavigateToTimer: () -> Unit = {}
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val tasks by viewModel.tasks.collectAsState()
    val focusTaskService: FocusTaskService = koinInject<FocusTaskService>()

    val windowInfo = currentWindowAdaptiveInfo()
    val coroutineScope = rememberCoroutineScope()
    val navigator = rememberSupportingPaneScaffoldNavigator<TaskScreens>()
    val padding = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) 0.dp else 16.dp
    val shape = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT)
        RectangleShape else MaterialTheme.shapes.medium

    // Auto-navigate to initial task if provided
    LaunchedEffect(initialTaskId, tasks) {
        initialTaskId?.let { taskId ->
            val task = tasks.find { it.id == taskId }
            task?.let {
                navigator.navigateTo(
                    ThreePaneScaffoldRole.Secondary,
                    TaskScreens.TaskDetails(task)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar("Tasks")
        },
        content = { paddings ->
            Box(modifier = Modifier.padding(paddings)) {
                SupportingPaneScaffold(
                    modifier = Modifier
                        .padding(start = padding, end = padding, bottom = padding),
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    mainPane = {
                        AnimatedPane {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = shape
                                    )
                            ) {
                                TaskList(
                                    tasks,
                                    onTaskClick = { task ->
                                        coroutineScope.launch {
                                            navigator.navigateTo(
                                                ThreePaneScaffoldRole.Secondary,
                                                TaskScreens.TaskDetails(task)
                                            )
                                        }
                                    },
                                    onTaskFocused = onNavigateToTimer
                                )
                            }
                        }
                    },
                    supportingPane = {
                        (navigator.currentDestination?.contentKey as? TaskScreens.TaskDetails)?.let {
                            AnimatedPane {
                                Column(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = shape
                                        ),
                                ) {
                                    val task = it.task

                                    key(task.id) {
                                        TaskDetails(
                                            task = task,
                                            onUpdatedTask = { updatedTask ->
                                                coroutineScope.launch {
                                                    viewModel.updateTask(updatedTask)
                                                    navigator.navigateTo(
                                                        ThreePaneScaffoldRole.Primary,
                                                        TaskScreens.TaskList
                                                    )
                                                }
                                            },
                                            onCancel = {
                                                coroutineScope.launch {
                                                    navigator.navigateTo(
                                                        ThreePaneScaffoldRole.Primary,
                                                        TaskScreens.TaskList
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskFocused: () -> Unit = {},
    isTimerRunning: Boolean = false
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val focusTaskService: FocusTaskService = koinInject<FocusTaskService>()
    val focusedTask by focusTaskService.focusedTask.collectAsState()

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
                        val success = focusTaskService.toggleTaskSelection(currentTask, isTimerRunning)
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion() }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { onFocusToggle() }
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
            modifier = Modifier.weight(1f),
            placeholder = { Text("Enter new task") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(onClick = {
            onAddTask(value)
            value = ""
        }) {
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
            Text(
                text = "Edit Task",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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

sealed class TaskScreens {

    @Serializable
    data class TaskDetails(
        val task: Task
    ) : TaskScreens()

    @Serializable
    data object TaskList : TaskScreens()
}


