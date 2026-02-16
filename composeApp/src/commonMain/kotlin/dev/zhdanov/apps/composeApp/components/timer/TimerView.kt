package dev.zhdanov.apps.composeApp.components.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.screens.feedback.FeedbackContent
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.shared.model.Task

// Константы для общих размеров
private val TIMER_SIZE = 250.dp
private val VERTICAL_SPACING = 16.dp
private val BUTTON_SPACING = 8.dp

/**
 * UI Component: FocusedTaskIndicator
 * Location: Above circular timer in TimerView
 * Shows: Focus icon + task title + clear button
 * Background: MaterialTheme.colorScheme.primaryContainer
 */
@Composable
private fun FocusedTaskIndicator(
    task: Task,
    onClear: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Focused task",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear focused task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * UI Component: SessionTasksIndicator
 * Shows all tasks in the current session (completed + active)
 */
@Composable
private fun SessionTasksIndicator(
    completedTasks: List<Task>,
    activeTask: Task?,
    onClearActive: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Session tasks",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (completedTasks.isNotEmpty() || activeTask != null) {
                        val count = completedTasks.size + (if (activeTask != null) 1 else 0)
                        "$count task${if (count > 1) "s" else ""} in session"
                    } else {
                        "No tasks"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Completed tasks (non-clickable, shown with checkmark)
            completedTasks.forEach { task ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 24.dp)
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Active task (highlighted, with clear button)
            activeTask?.let { task ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Active task",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearActive,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear active task",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Основной компонент таймера, включающий в себя круговой таймер и кнопки управления
 */
@OptIn(KoinExperimentalAPI::class, ExperimentalTime::class)
@Composable
@Preview
fun TimerView() {
    val viewModel = koinInject<TimerViewModel>()
    val focusTaskService = koinInject<FocusTaskService>()
    val isPaused = viewModel.isPause.collectAsState()
    val isRunning = viewModel.isRunning.collectAsState()
    val time = viewModel.time.collectAsState("")
    val progress = viewModel.progress.collectAsState()
    val state = viewModel.state.collectAsState()
    val settingList = viewModel.settingList.collectAsState(listOf<TimerSettings>())
    val lastPartDuration = viewModel.lastPartDuration.collectAsState()
    val focusedTask by focusTaskService.focusedTask.collectAsState()
    val completedTasks by focusTaskService.completedTasks.collectAsState()

    // Определение цветов в зависимости от состояния таймера
    val accentColor = getAccentColorForState(state.value)

    // Показываем экран обратной связи, если в соответствующем состоянии
    if (state.value == TimerViewState.FEEDBACK) {
        FeedbackContent(
            duration = lastPartDuration.value.toInt(),
            completedTasks = completedTasks,
            activeTask = focusedTask,
            onSubmit = { feedback ->
                feedback?.let { viewModel.saveFeedback(it) }
                viewModel.closeFeedback()
            }
        )
    } else {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Компонент кругового таймера
            TimerCircle(
                progress = progress.value,
                timeString = time.value,
                state = state.value,
                settingList = settingList.value,
                accentColor = accentColor,
                isRunning = isRunning.value,
                onSettingsChange = { viewModel.changeTimerSettings(it) }
            )

            Spacer(modifier = Modifier.height(VERTICAL_SPACING))

            // Блок кнопок управления таймером
            TimerControlButtons(
                isRunning = isRunning.value,
                isPaused = isPaused.value,
                state = state.value,
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onStop = { viewModel.stopTimer() },
                onSkip = { viewModel.skipTimer() }
            )

            // Show session tasks indicator if there are any tasks in the session
            if (completedTasks.isNotEmpty() || focusedTask != null) {
                Spacer(modifier = Modifier.height(VERTICAL_SPACING))
                SessionTasksIndicator(
                    completedTasks = completedTasks,
                    activeTask = focusedTask,
                    onClearActive = { focusTaskService.clearFocusedTask() }
                )
            }
        }
    }
}

/**
 * Компонент кругового таймера с настройками
 */
@Composable
private fun TimerCircle(
    progress: Float,
    timeString: String,
    state: TimerViewState,
    settingList: List<TimerSettings>,
    accentColor: androidx.compose.ui.graphics.Color,
    isRunning: Boolean,
    onSettingsChange: (TimerSettings) -> Unit
) {
    Box(
        modifier = Modifier.size(TIMER_SIZE),
    ) {
        CircularCountdownTimer(
            progress = progress,
            timeString = timeString,
            label = state.toString().capitalize(Locale.current),
            settingList = settingList,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            accentColor = accentColor,
            editable = !isRunning,
            onChange = onSettingsChange
        )
    }
}

/**
 * Компонент для отображения кнопок управления таймером
 */
@Composable
private fun TimerControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    state: TimerViewState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    Row {
        if (!isRunning) {
            StartButton(
                state = state,
                onClick = onStart
            )
        } else {
            ControlButtonsGroup(
                isPaused = isPaused,
                state = state,
                onPause = onPause,
                onStop = onStop,
                onSkip = onSkip
            )
        }
    }
}

/**
 * Кнопка запуска таймера
 */
@Composable
private fun StartButton(
    state: TimerViewState,
    onClick: () -> Unit
) {
    val buttonColors = getStartButtonColors(state)

    Button(
        colors = buttonColors,
        enabled = state != TimerViewState.FEEDBACK,
        onClick = onClick
    ) {
        Text("Start")
    }
}

/**
 * Группа кнопок управления для активного таймера
 */
@Composable
private fun ControlButtonsGroup(
    isPaused: Boolean,
    state: TimerViewState,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit
) {
    // Кнопка паузы/продолжения
    PauseResumeButton(
        isPaused = isPaused,
        onClick = onPause
    )

    Spacer(modifier = Modifier.width(BUTTON_SPACING))

    // Кнопка остановки или пропуска в зависимости от состояния
    if (state == TimerViewState.WORK) {
        StopButton(onClick = onStop)
    } else {
        SkipButton(onClick = onSkip)
    }
}

/**
 * Кнопка паузы/возобновления
 */
@Composable
private fun PauseResumeButton(
    isPaused: Boolean,
    onClick: () -> Unit
) {
    val colors = if (isPaused) {
        buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    Button(
        colors = colors,
        onClick = onClick
    ) {
        Text(if (isPaused) "Resume" else "Pause")
    }
}

/**
 * Кнопка остановки таймера
 */
@Composable
private fun StopButton(onClick: () -> Unit) {
    Button(
        colors = buttonColors(
            contentColor = MaterialTheme.colorScheme.onError,
            containerColor = MaterialTheme.colorScheme.error
        ),
        onClick = onClick
    ) {
        Text("Stop")
    }
}

/**
 * Кнопка пропуска таймера
 */
@Composable
private fun SkipButton(onClick: () -> Unit) {
    OutlinedButton(
        colors = outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = onClick
    ) {
        Text("Skip")
    }
}

/**
 * Возвращает акцентный цвет в зависимости от состояния таймера
 */
@Composable
private fun getAccentColorForState(state: TimerViewState): androidx.compose.ui.graphics.Color {
    return if (state == TimerViewState.WORK)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.tertiary
}

/**
 * Возвращает цвета для кнопки запуска в зависимости от состояния
 */
@Composable
private fun getStartButtonColors(state: TimerViewState) = if (state == TimerViewState.WORK)
    buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
else
    buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary
    )
