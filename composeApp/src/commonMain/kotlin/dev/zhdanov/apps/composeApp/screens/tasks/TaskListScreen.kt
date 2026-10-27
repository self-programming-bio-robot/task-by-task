package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.AppPane
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.services.TimerSessionService
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLocalDate
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(KoinExperimentalAPI::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListScreen(
    initialTaskId: Long? = null,
    onNavigateToTimer: () -> Unit = {},
    onTaskClick: (Long) -> Unit = {},
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val groups by viewModel.groupedTasks.collectAsState()
    val today by viewModel.today.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Auto-navigate to initial task if provided
    LaunchedEffect(initialTaskId) {
        initialTaskId?.let(onTaskClick)
    }

    AppPane(
        title = Screen.TaskList().title,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.testTag(UiTestTags.TaskListScreen),
        actions = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 220.dp)
                    .testTag(UiTestTags.TaskSearchInput),
                placeholder = {
                    Text(
                        "Search",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = filter.todayOnly,
                    onClick = { viewModel.updateFilter { it.copy(todayOnly = !it.todayOnly) } },
                    label = {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    modifier = Modifier.testTag(UiTestTags.QuickFilterToday),
                )
                FilterChip(
                    selected = filter.overdueOnly,
                    onClick = { viewModel.updateFilter { it.copy(overdueOnly = !it.overdueOnly) } },
                    label = {
                        Text(
                            "Overdue",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    modifier = Modifier.testTag(UiTestTags.QuickFilterOverdue),
                )
                FilterChip(
                    selected = TaskTypeFilter.REGULAR in filter.taskTypes,
                    onClick = {
                        viewModel.updateFilter { current ->
                            val updated = if (TaskTypeFilter.REGULAR in current.taskTypes) {
                                current.taskTypes - TaskTypeFilter.REGULAR
                            } else {
                                current.taskTypes + TaskTypeFilter.REGULAR
                            }
                            current.copy(taskTypes = updated)
                        }
                    },
                    label = {
                        Text(
                            "Regular",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    modifier = Modifier.testTag(UiTestTags.QuickFilterRegular),
                )
                FilterChip(
                    selected = TaskTypeFilter.ROUTINE in filter.taskTypes,
                    onClick = {
                        viewModel.updateFilter { current ->
                            val updated = if (TaskTypeFilter.ROUTINE in current.taskTypes) {
                                current.taskTypes - TaskTypeFilter.ROUTINE
                            } else {
                                current.taskTypes + TaskTypeFilter.ROUTINE
                            }
                            current.copy(taskTypes = updated)
                        }
                    },
                    label = {
                        Text(
                            "Routines",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    modifier = Modifier.testTag(UiTestTags.QuickFilterRoutines),
                )
                FilterChip(
                    selected = filter.showCompleted,
                    onClick = { viewModel.updateFilter { it.copy(showCompleted = !it.showCompleted) } },
                    label = {
                        Text(
                            "Completed",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    modifier = Modifier.testTag(UiTestTags.QuickFilterCompleted),
                )
                if (!filter.isDefault || searchQuery.isNotBlank()) {
                    TextButton(
                        onClick = {
                            viewModel.setSearchQuery("")
                            viewModel.updateFilter { TaskListFilter() }
                        }
                    ) {
                        Text("Reset", maxLines = 1, softWrap = false)
                    }
                }
            }
            TaskList(
                groups = groups,
                today = today,
                onTaskClick = { onTaskClick(it.id) },
                onTaskFocused = onNavigateToTimer,
            )
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
    val today by viewModel.today.collectAsState()
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
                    today = today,
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
    groups: List<TaskGroup>,
    today: LocalDate,
    onTaskClick: (Task) -> Unit,
    onTaskFocused: () -> Unit = {}
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val focusTaskService: FocusTaskService = koinInject<FocusTaskService>()
    val timerSessionService: TimerSessionService = koinInject<TimerSessionService>()
    val focusedTask by focusTaskService.focusedTask.collectAsState()
    val isTimerRunning by timerSessionService.isRunning.collectAsState()
    val todayLong = today.toLong()

    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item(key = "new-task-input") {
                NewTaskInput(
                    onAddTask = { viewModel.addNewTask(it) },
                    onAddTaskWithDate = viewModel::addNewTask,
                    today = today,
                )
            }
            if (groups.isEmpty()) {
                item(key = "empty-tasks") {
                    Text(
                        text = "No matching tasks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            } else {
                groups.forEach { group ->
                    val headerKey = "header-${group.date ?: "none"}"
                    val isOverdueGroup = group.date != null && group.date < todayLong
                    item(key = headerKey) {
                        Text(
                            text = formatGroupDateHeader(group.date, today),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isOverdueGroup) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                        )
                    }
                    items(group.tasks, key = { it.id }) { task ->
                        val currentTask = task
                        TaskItem(
                            task = currentTask,
                            isFocused = focusedTask?.id == currentTask.id,
                            today = today,
                            onToggleCompletion = {
                                viewModel.toggleTaskCompletion(currentTask)
                                if (focusedTask?.id == currentTask.id) {
                                    focusTaskService.updateFocusedTask(currentTask.copy(isCompleted = !currentTask.isCompleted))
                                }
                            },
                            onAddToday = { dueToday ->
                                viewModel.setDueToday(currentTask, dueToday)
                            },
                            onFocusToggle = {
                                if (currentTask.routineId == null && !currentTask.isDueOn(today)) {
                                    viewModel.setDueToday(currentTask, true)
                                }
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
    today: LocalDate? = null,
) {
    val isDueToday = if (today != null) task.isDueOn(today) else task.targetDate != null

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
            checked = isDueToday,
            onCheckedChange = { onAddToday(it) },
            enabled = task.routineId == null,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun NewTaskInput(
    onAddTask: (value: String) -> Unit,
    onAddTaskWithDate: ((value: String, targetDate: LocalDate?) -> Unit)? = null,
    today: LocalDate? = null,
) {
    var value by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AutoSelectDatePickerDialog(
            initialSelectedDate = selectedDate,
            initialDisplayedDate = selectedDate ?: today,
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

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
            trailingIcon = if (onAddTaskWithDate != null) {
                {
                    val date = selectedDate
                    if (date == null) {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Set due date",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            TextButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (today != null) {
                                        formatGroupDateHeader(date.toLong(), today)
                                    } else {
                                        formatShortDate(date, includeYear = false)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                            IconButton(
                                onClick = { selectedDate = null },
                                modifier = Modifier
                                    .size(28.dp)
                                    .pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear due date",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                null
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = {
                val title = value.trim()
                if (title.isNotEmpty()) {
                    if (onAddTaskWithDate != null) {
                        onAddTaskWithDate(title, selectedDate)
                    } else {
                        onAddTask(title)
                    }
                }
                value = ""
                selectedDate = null
            },
            modifier = Modifier.testTag(UiTestTags.AddTaskButton)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add task")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun TaskDetails(
    task: Task,
    onUpdatedTask: (task: Task) -> Unit,
    onConvertToRoutine: () -> Unit = {},
    onDeleteTask: () -> Unit = {},
    onCancel: () -> Unit,
    today: LocalDate? = null,
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description.orEmpty()) }
    var targetDate by remember { mutableStateOf(task.targetDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val currentDate = remember(targetDate) { targetDate?.toLocalDate() }
        AutoSelectDatePickerDialog(
            initialSelectedDate = currentDate,
            initialDisplayedDate = currentDate ?: today,
            onDateSelected = { pickedDate ->
                targetDate = pickedDate.toLong()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

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
                Text(text = "Due date")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        enabled = task.routineId == null,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = targetDate?.let { dateLong ->
                                if (today != null) {
                                    formatGroupDateHeader(dateLong, today)
                                } else {
                                    formatShortDate(dateLong.toLocalDate())
                                }
                            } ?: "No date",
                            maxLines = 1,
                        )
                    }
                    if (targetDate != null && task.routineId == null) {
                        IconButton(onClick = { targetDate = null }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear due date"
                            )
                        }
                    }
                }
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
                        targetDate = task.targetDate

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
                                targetDate = targetDate
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun AutoSelectDatePickerDialog(
    initialSelectedDate: LocalDate?,
    initialDisplayedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val systemToday = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val initialSelectedMillis = remember(initialSelectedDate) {
        initialSelectedDate
            ?.atStartOfDayIn(TimeZone.UTC)
            ?.toEpochMilliseconds()
    }
    val initialDisplayedMillis = remember(initialDisplayedDate, systemToday) {
        (initialDisplayedDate ?: systemToday)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedMillis,
        initialDisplayedMonthMillis = initialDisplayedMillis,
    )
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis
        if (millis != null && millis != initialSelectedMillis) {
            val pickedDate = Instant.fromEpochMilliseconds(millis)
                .toLocalDateTime(TimeZone.UTC)
                .date
            onDateSelected(pickedDate)
        }
    }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

internal fun formatGroupDateHeader(dateLong: Long?, today: LocalDate): String {
    if (dateLong == null) return "No date"
    val date = dateLong.toLocalDate()
    return when (date) {
        today -> "Today"
        today.plus(1, DateTimeUnit.DAY) -> "Tomorrow"
        today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
        else -> formatShortDate(date, includeYear = date.year != today.year)
    }
}

private val SHORT_MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private val SHORT_DAYS = arrayOf(
    "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
)

private fun formatShortDate(date: LocalDate, includeYear: Boolean = true): String {
    val dow = SHORT_DAYS[date.dayOfWeek.ordinal]
    val month = SHORT_MONTHS[date.month.ordinal]
    return if (includeYear) {
        "$dow, ${date.day} $month ${date.year}"
    } else {
        "$dow, ${date.day} $month"
    }
}
