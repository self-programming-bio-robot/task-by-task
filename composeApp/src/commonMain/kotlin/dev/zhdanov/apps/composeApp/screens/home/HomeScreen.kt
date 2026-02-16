package dev.zhdanov.apps.composeApp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.timer.TimerView
import dev.zhdanov.apps.composeApp.components.timer.TimerViewModel
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import dev.zhdanov.apps.composeApp.screens.tasks.NewTaskInput
import dev.zhdanov.apps.composeApp.screens.tasks.TaskListViewModel
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun HomeScreen(
    onFinishDay: (review: AssistantReviewResponse) -> Unit
) {
    val viewModel = koinViewModel<HomeViewModel>()

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val isActive by viewModel.isActive.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                title = "Home",
                hasBack = false,
                onBack = {},
                actions = {
                    IconButton(
                        enabled = isActive,
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                onFinishDay(viewModel.finishDay())
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                            contentDescription = "Finish day"
                        )
                    }
                }
            )
        },
        content = { paddings ->
            Column(
                modifier = Modifier
                    .padding(paddings)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Timer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimerView()

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { }
                                )
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }

                // Task list
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "For today",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    TodayTaskList(
                        onTaskClick = {},
                        onTaskFocused = {}
                    )
                }
            }
        }
    )
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TodayTaskList(
    onTaskClick: (Task) -> Unit,
    onTaskFocused: () -> Unit = {},
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val focusTaskService: FocusTaskService = koinInject<FocusTaskService>()
    val timerViewModel: TimerViewModel = koinInject<TimerViewModel>()
    val todayTasks by viewModel.todayTask.collectAsState(listOf())
    val focusedTask by focusTaskService.focusedTask.collectAsState()
    val isTimerRunning by timerViewModel.isRunning.collectAsState()


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            NewTaskInput(
                onAddTask = viewModel::addTodayTask
            )
        }
        if (todayTasks.isEmpty()) {
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    text = "No tasks for today"
                )
            }
        } else {
            items(todayTasks, key = { it.id }) { task ->
                val currentTask = task // Capture for lambda
                TodayTaskItem(
                    task = currentTask,
                    isFocused = focusedTask?.id == currentTask.id,
                    isTimerRunning = isTimerRunning,
                    onToggleCompletion = {
                        // Toggle completion in DB
                        viewModel.toggleTaskCompletion(currentTask)
                        // Update focused task if this is the focused one
                        if (focusedTask?.id == currentTask.id) {
                            focusTaskService.updateFocusedTask(currentTask.copy(isCompleted = !currentTask.isCompleted))
                        }
                    },
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
fun TodayTaskItem(
    task: Task,
    isFocused: Boolean,
    isTimerRunning: Boolean,
    onToggleCompletion: () -> Unit,
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
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = { onFocusToggle() }
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = if (isFocused) "Unfocus task" else "Focus task",
                tint = if (isFocused) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
