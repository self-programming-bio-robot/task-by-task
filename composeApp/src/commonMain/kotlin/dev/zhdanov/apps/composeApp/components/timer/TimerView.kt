package dev.zhdanov.apps.composeApp.components.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.screens.feedback.Feedback
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

// Константы для общих размеров
private val TIMER_SIZE = 250.dp
private val VERTICAL_SPACING = 16.dp
private val BUTTON_SPACING = 8.dp

/**
 * Основной компонент таймера, включающий в себя круговой таймер и кнопки управления
 */
@OptIn(KoinExperimentalAPI::class, ExperimentalTime::class)
@Composable
@Preview
fun TimerView() {
    val viewModel = koinInject<TimerViewModel>()
    val isPaused = viewModel.isPause.collectAsState()
    val isRunning = viewModel.isRunning.collectAsState()
    val time = viewModel.time.collectAsState("")
    val progress = viewModel.progress.collectAsState()
    val state = viewModel.state.collectAsState()
    val settingList = viewModel.settingList.collectAsState(listOf<TimerSettings>())
    val lastPartDuration = viewModel.lastPartDuration.collectAsState()

    // Определение цветов в зависимости от состояния таймера
    val accentColor = getAccentColorForState(state.value)

    // Показываем экран обратной связи, если в соответствующем состоянии
    if (state.value == TimerViewState.FEEDBACK) {
        FeedbackScreen(
            lastPartDuration = lastPartDuration.value.toInt(),
            onExit = { feedback ->
                feedback?.let { viewModel.saveFeedback(it) }
                viewModel.closeFeedback()
            }
        )
    }

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
    }
}

/**
 * Компонент для отображения экрана обратной связи
 */
@Composable
@OptIn(ExperimentalTime::class)
private fun FeedbackScreen(
    lastPartDuration: Int,
    onExit: (CreateFocusTime?) -> Unit
) {
    Feedback(
        finishAt = Clock.System.now().toEpochMilliseconds(),
        duration = lastPartDuration,
        onExit = onExit
    )
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
