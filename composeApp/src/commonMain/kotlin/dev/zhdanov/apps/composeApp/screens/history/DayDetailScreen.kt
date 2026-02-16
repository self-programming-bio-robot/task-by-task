package dev.zhdanov.apps.composeApp.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTimeWithTasks
import dev.zhdanov.apps.shared.model.Task
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.time.ExperimentalTime

@OptIn(KoinExperimentalAPI::class, ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    onBack: () -> Unit,
    onNavigateToTask: (Long) -> Unit = {}
) {
    val viewModel: HistoryViewModel = koinViewModel()
    val daySummary = remember { viewModel.getDaySummary(date) }
    val focusTimes = remember { viewModel.getFocusTimesWithTasksForDate(date) }

    Scaffold(
        topBar = {
            TopBar(
                title = "Day Review - $date",
                hasBack = true,
                onBack = { onBack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Day Summary Section
                daySummary?.let { summary ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Day Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Markdown(content = summary.review)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total focus time: ${summary.focusTime / 60} minutes",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Focus Sessions Section
                item {
                    Text(
                        text = "Focus Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (focusTimes.isEmpty()) {
                    item {
                        Text(
                            text = "No focus sessions recorded for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    items(focusTimes) { item ->
                        FocusSessionCard(
                            focusTimeWithTasks = item,
                            onTaskClick = onNavigateToTask
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusSessionCard(
    focusTimeWithTasks: FocusTimeWithTasks,
    onTaskClick: (Long) -> Unit
) {
    val focusTime = focusTimeWithTasks.focusTime
    val tasks = focusTimeWithTasks.tasks

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp)
    ) {
        // Time and duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notes,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${focusTime.duration / 60} min",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Linked Tasks (can be multiple)
        if (tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tasks:",
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tasks.forEach { task ->
                    TaskChip(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }

        // Feedback/Note
        if (focusTime.feedback.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = focusTime.feedback,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TaskChip(
    task: Task,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}
