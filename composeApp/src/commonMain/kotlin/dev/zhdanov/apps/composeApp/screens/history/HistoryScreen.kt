package dev.zhdanov.apps.composeApp.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.components.history.HistoryList
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.model.FocusTimeWithTasks
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.time.ExperimentalTime

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun HistoryScreen(
    onNavigateToTask: (Long) -> Unit = {},
    onNavigateToDayDetail: (LocalDate) -> Unit = {}
) {
    val viewModel: HistoryViewModel = koinViewModel<HistoryViewModel>()

    val history = viewModel.history.collectAsState(listOf())
    val isLoading = viewModel.isLoading.collectAsState(initial = false)
    val windowInfo = currentWindowAdaptiveInfo()
    val navigator = rememberSupportingPaneScaffoldNavigator<LocalDate>()

    val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM
    val padding = if (isCompact) 0.dp else 16.dp
    val shape = if (isCompact) RectangleShape else MaterialTheme.shapes.medium

    // Selected day for detail view (only used in wide mode)
    var selectedDay by remember { mutableStateOf<DaySummary?>(null) }
    var focusTimesForDay by remember { mutableStateOf<List<FocusTimeWithTasks>>(emptyList()) }

    // Handle day click based on screen size
    val handleDayClick: (DaySummary) -> Unit = { daySummary ->
        if (isCompact) {
            // On compact screens, navigate to detail screen
            onNavigateToDayDetail(daySummary.date)
        } else {
            // On wide screens, show in supporting pane
            selectedDay = daySummary
            focusTimesForDay = viewModel.getFocusTimesWithTasksForDate(daySummary.date)
        }
    }

    Scaffold(
        topBar = {
            TopBar("History")
        },
        content = { paddings ->
            Box(modifier = Modifier.padding(paddings)) {
                if (isCompact) {
                    // Compact mode: just show the list
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = shape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp)
                            )
                        } else {
                            HistoryList(
                                items = history.value,
                                onDayClick = handleDayClick
                            )
                        }
                    }
                } else {
                    // Wide mode: show supporting pane scaffold
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
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoading.value) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(50.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HistoryList(
                                                items = history.value,
                                                onDayClick = handleDayClick
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        supportingPane = {
                            AnimatedPane {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = shape
                                        ),
                                ) {
                                    selectedDay?.let { day ->
                                        DayNotesList(
                                            daySummary = day,
                                            focusTimes = focusTimesForDay,
                                            onNavigateToTask = onNavigateToTask
                                        )
                                    } ?: run {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Select a day to view notes",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun DayNotesList(
    daySummary: DaySummary,
    focusTimes: List<FocusTimeWithTasks>,
    onNavigateToTask: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Notes for ${daySummary.date}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Focus sessions with notes
        if (focusTimes.isEmpty()) {
            Text(
                text = "No focus sessions recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(focusTimes) { item ->
                    NoteCard(
                        focusTimeWithTasks = item,
                        onTaskClick = onNavigateToTask
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    focusTimeWithTasks: FocusTimeWithTasks,
    onTaskClick: (Long) -> Unit
) {
    val focusTime = focusTimeWithTasks.focusTime
    val tasks = focusTimeWithTasks.tasks

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
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
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Linked Tasks (can be multiple)
            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
