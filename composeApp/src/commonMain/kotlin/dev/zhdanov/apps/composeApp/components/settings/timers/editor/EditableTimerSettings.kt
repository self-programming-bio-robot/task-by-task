package dev.zhdanov.apps.composeApp.components.settings.timers.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.shared.DEFAULT_TIMER_SETTINGS
import dev.zhdanov.apps.shared.model.TimerSettings
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.math.roundToInt

@OptIn(KoinExperimentalAPI::class)
@Composable
fun EditableTimerSettings(
    timerSettings: TimerSettings?,
    onBack: () -> Unit,
) {
    val initialState = timerSettings ?: DEFAULT_TIMER_SETTINGS
    val disabled: Boolean = timerSettings?.id == DEFAULT_TIMER_SETTINGS.id

    var workDuration by remember { mutableStateOf(initialState.workDuration.toFloat() / 60) }
    var shortBreakDuration by remember { mutableStateOf(initialState.shortBreakDuration.toFloat() / 60) }
    var longBreakDuration by remember { mutableStateOf(initialState.longBreakDuration.toFloat() / 60) }
    var workCycles by remember { mutableStateOf(initialState.workCycles.toFloat()) }

    val viewModel = koinViewModel<EditableTimerSettingsViewModel>()

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
//        Text(
//            text = "Timer Settings",
//            style = MaterialTheme.typography.headlineMedium,
//        )

        // Work Duration Slider
        Text(
            text = "Work Duration: ${workDuration.roundToInt()} min",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = workDuration,
            onValueChange = {
                workDuration = it
            },
            valueRange = 5f..120f,
            steps = 22,
            enabled = !disabled,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Short Break Duration Slider
        Text(
            text = "Short Break Duration: ${shortBreakDuration.roundToInt()} min",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = shortBreakDuration,
            onValueChange = {
                shortBreakDuration = it
            },
            valueRange = 1f..30f,
            steps = 28,
            enabled = !disabled,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Long Break Duration Slider
        Text(
            text = "Long Break Duration: ${longBreakDuration.roundToInt()} min",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = longBreakDuration,
            onValueChange = {
                longBreakDuration = it
            },
            valueRange = 5f..60f,
            steps = 54,
            enabled = !disabled,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Work Cycles Slider
        Text(
            text = "Work Cycles: ${workCycles.roundToInt()}",
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = workCycles,
            onValueChange = {
                workCycles = it
            },
            valueRange = 0f..10f,
            steps = 9,
            enabled = !disabled,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(
                onClick = { onBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    viewModel.updateTimerSettings(
                        id = timerSettings?.id,
                        workDuration = workDuration.roundToInt() * 60,
                        shortBreakDuration = shortBreakDuration.roundToInt() * 60,
                        longBreakDuration = longBreakDuration.roundToInt() * 60,
                        workCycles = workCycles.roundToInt(),
                    )
                    onBack()
                },
                enabled = !disabled,
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

    }
}
