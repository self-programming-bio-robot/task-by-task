package dev.zhdanov.apps.composeApp.components.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zhdanov.apps.shared.model.TimerSettings
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@Composable
fun CircularCountdownTimer(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    strokeWidth: Dp = 16.dp,
    textColor: Color = MaterialTheme.typography.headlineLarge.color,
    label: String = "Work",
    labelColor: Color = MaterialTheme.typography.titleLarge.color,
    progress: Float = 0f,
    timeString: String,
    editable: Boolean = true,
    settingList: List<TimerSettings>,
    onChange: (timerSettings: TimerSettings) -> Unit
) {
    val animatedProgress = 1 - progress
    val displayTime = timeString

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height)
            val stroke = strokeWidth.toPx()
            val arcRect = Rect(
                (size.width - diameter) / 2,
                (size.height - diameter) / 2,
                (size.width + diameter) / 2,
                (size.height + diameter) / 2
            )
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = -360f * animatedProgress,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SelectionView(
                time = displayTime,
                editable = editable,
                settingList = settingList,
                textColor = textColor,
                onChange = onChange
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = labelColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionView(
    time: String,
    editable: Boolean,
    textColor: Color = MaterialTheme.typography.headlineLarge.color,
    settingList: List<TimerSettings>,
    onChange: (timerSettings: TimerSettings) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    fun TimerSettings.formatted(): String {
        val workDuration = if (workDuration < 0) "∞" else workDuration.seconds.toString()
        val shortBreakDuration = if (shortBreakDuration < 0) "∞" else shortBreakDuration.seconds.toString()
        val longBreakDuration = if (longBreakDuration < 0) "∞" else longBreakDuration.seconds.toString()

        return "$workCycles $workDuration | $shortBreakDuration | $longBreakDuration"
    }

    Box(modifier = Modifier) {
        TextButton(
            enabled = editable,
            onClick = { expanded = !expanded },
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.headlineLarge,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            settingList.map {
                DropdownMenuItem(
                    text = { Text(it.formatted()) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
