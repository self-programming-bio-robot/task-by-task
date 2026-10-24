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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.AppPane
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.services.TimerSessionService
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.model.Routine
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

    LaunchedEffect(initialTaskId) {
        initialTaskId?.let(onTaskClick)
    }

    AppPane(
        title = Screen.TaskList().title,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.testTag(UiTestTags.TaskListScreen),
    ) {
        TaskList(
            tasks = tasks,
            onTaskClick = { onTaskClick(it.id) },
            onTaskFocused = onNavigateToTimer
        )
    }
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskRoutinesPane(
    onRoutineClick: (Long) -> Unit = {},
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val routines by viewModel.routines.collectAsState()

    AppPane(
        title = Screen.TaskRoutines.title,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.testTag(UiTestTags.TaskRoutinesScreen),
    ) {
        RoutineList(
            routines = routines,
            onAddRoutine = { title -> viewModel.addNewRoutine(title) },
            onRoutineClick = { onRoutineClick(it.id) },
            onToggleActive = { viewModel.toggleRoutineActive(it.id) },
            onDelete = { viewModel.deleteRoutine(it.id) }
        )
    }
}

@Composable
fun RoutineList(
    routines: List<Routine>,
    onAddRoutine: (String) -> Unit,
    onRoutineClick: (Routine) -> Unit = {},
    onToggleActive: (Routine) -> Unit,
    onDelete: (Routine) -> Unit,
) {
    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                NewRoutineInput(onAddRoutine = onAddRoutine)
            }
            if (routines.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = "No routines yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(routines, key = { it.id }) { routine ->
                    RoutineItem(
                        routine = routine,
                        onClick = { onRoutineClick(routine) },
                        onToggleActive = { onToggleActive(routine) },
                        onDelete = { onDelete(routine) }
                    )
                }
            }
        }
    }
}

@Composable
fun NewRoutineInput(
    onAddRoutine: (value: String) -> Unit
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
            modifier = Modifier
                .weight(1f)
                .testTag(UiTestTags.NewRoutineInput),
            placeholder = { Text("Enter new daily routine") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = {
                val title = value.trim()
                if (title.isNotEmpty()) {
                    onAddRoutine(title)
                }
                value = ""
            },
            modifier = Modifier.testTag(UiTestTags.AddRoutineButton)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add routine")
        }
    }
}

@Composable
fun RoutineItem(
    routine: Routine,
    onClick: () -> Unit = {},
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.routineRow(routine.id))
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = "Daily Routine",
            tint = if (routine.isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = routine.title,
                style = MaterialTheme.typography.bodyLarge
            )
            val desc = routine.description
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = routine.isActive,
            onCheckedChange = { onToggleActive() },
            modifier = Modifier.testTag(UiTestTags.routineActiveToggle(routine.id))
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag(UiTestTags.routineDeleteButton(routine.id))
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete routine",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Extra pane that edits a single routine template identified by [routineId].
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun RoutineEditPane(
    routineId: Long,
    onDone: () -> Unit,
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val routines by viewModel.routines.collectAsState()
    val routine = routines.find { it.id == routineId }
    val coroutineScope = rememberCoroutineScope()

    AppPane(
        title = "Edit routine",
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        if (routine == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Routine not found")
            }
        } else {
            key(routineId) {
                RoutineDetails(
                    routine = routine,
                    onUpdatedRoutine = { updatedRoutine ->
                        coroutineScope.launch {
                            viewModel.updateRoutine(updatedRoutine)
                            onDone()
                        }
                    },
                    onDeleteRoutine = {
                        coroutineScope.launch {
                            viewModel.deleteRoutine(routine.id)
                            onDone()
                        }
                    },
                    onCancel = onDone
                )
            }
        }
    }
}

@Composable
fun RoutineDetails(
    routine: Routine,
    onUpdatedRoutine: (routine: Routine) -> Unit,
    onDeleteRoutine: () -> Unit = {},
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(routine.title) }
    var description by remember { mutableStateOf(routine.description.orEmpty()) }
    var isActive by remember { mutableStateOf(routine.isActive) }

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
                maxLines = Int.MAX_VALUE
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Active")
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDeleteRoutine,
                modifier = Modifier.testTag(UiTestTags.RoutineDeleteInEditorButton)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete routine",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        title = routine.title
                        description = routine.description.orEmpty()
                        isActive = routine.isActive
                        onCancel()
                    }
                ) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onUpdatedRoutine(
                            routine.copy(
                                title = title.trim(),
                                description = description.trim().takeIf { it.isNotBlank() },
                                isActive = isActive
                            )
                        )
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * Supporting pane that edits a single task identified by [taskId].
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskEditPane(
    taskId: Long,
    onDone: () -> Unit,
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val focusTaskService: FocusTaskService = koinInject<FocusTaskService>()
    val tasks by viewModel.tasks.collectAsState()
    val focusedTask by focusTaskService.focusedTask.collectAsState()
    val task = tasks.find { it.id == taskId }
    val coroutineScope = rememberCoroutineScope()

    AppPane(
        title = "Edit task",
        color = MaterialTheme.colorScheme.tertiaryContainer,
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
                            if (focusedTask?.id == updatedTask.id) {
                                focusTaskService.updateFocusedTask(updatedTask)
                            }
                            onDone()
                        }
                    },
                    onConvertToRoutine = {
                        coroutineScope.launch {
                            viewModel.convertTaskToRoutine(task)
                            onDone()
                        }
                    },
                    onDeleteTask = {
                        coroutineScope.launch {
                            if (focusedTask?.id == task.id) {
                                focusTaskService.setFocusedTask(null)
                            }
                            viewModel.deleteTask(task.id)
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
                    onDelete = {
                        if (focusedTask?.id == currentTask.id) {
                            focusTaskService.setFocusedTask(null)
                        }
                        viewModel.deleteTask(currentTask.id)
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
    onDelete: () -> Unit = {},
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.taskRow(task.id))
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge)
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
        if (task.routineId != null) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Routine task",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge
            )
            val desc = task.description
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
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
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag(UiTestTags.taskDelete(task.id))
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete task",
                tint = MaterialTheme.colorScheme.error
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
fun TaskDetails(
    task: Task,
    onUpdatedTask: (task: Task) -> Unit,
    onConvertToRoutine: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
    onCancel: () -> Unit
) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Add to Today")
                Switch(
                    checked = isToday,
                    onCheckedChange = { isToday = it }
                )
            }
            if (task.routineId == null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onConvertToRoutine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.ConvertToRoutineButton)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Make Daily Routine", maxLines = 1, softWrap = false)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDeleteTask,
                modifier = Modifier.testTag(UiTestTags.TaskDeleteButton)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        title = task.title
                        description = task.description.orEmpty()
                        isToday = task.isToday

                        onCancel()
                    }
                ) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onUpdatedTask(
                            task.copy(
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                isToday = isToday
                            )
                        )
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
