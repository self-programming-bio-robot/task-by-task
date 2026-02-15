package dev.zhdanov.apps.composeApp.screens.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
actual fun Feedback(
    finishAt: Long,
    duration: Int,
    onExit: (feedback: CreateFocusTime?) -> Unit
) {
    val focusTaskService = koinInject<FocusTaskService>()
    val focusedTask by focusTaskService.focusedTask.collectAsState()
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Focus session complete!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Duration: ${duration / 60} minutes",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Show focused task if present
        focusedTask?.let { task ->
            Spacer(modifier = Modifier.height(12.dp))
            FocusedTaskIndicatorIos(
                task = task,
                onClear = { focusTaskService.clearFocusedTask() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback text field
        OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            label = { Text("How did it go?") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            OutlinedButton(onClick = { onExit(null) }) {
                Text("Skip")
            }
            Button(
                onClick = {
                    onExit(CreateFocusTime(
                        duration = duration,
                        feedback = feedbackText,
                        finishedAt = finishAt
                    ))
                }
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun FocusedTaskIndicatorIos(
    task: Task,
    onClear: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Focused task",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
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
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear focused task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
