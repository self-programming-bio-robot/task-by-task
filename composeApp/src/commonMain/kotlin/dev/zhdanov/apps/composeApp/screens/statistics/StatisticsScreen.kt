package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

enum class StatisticsPeriod {
    DAY,
    WEEK,
    MONTH
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun StatisticsScreen() {
    val viewModel: StatisticsViewModel = koinViewModel()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val focusTimeData by viewModel.focusTimeData.collectAsState()
    val workCyclesData by viewModel.workCyclesData.collectAsState()
    val tasksCreatedData by viewModel.tasksCreatedData.collectAsState()
    val tasksDoneData by viewModel.tasksDoneData.collectAsState()

    Scaffold(
        topBar = { TopBar("Statistics") }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Selection
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodChange = { viewModel.setPeriod(it) }
            )

            // Statistics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Focus Time",
                    value = formatDuration(focusTimeData),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Work Cycles",
                    value = workCyclesData.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Tasks Created",
                    value = tasksCreatedData.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Tasks Done",
                    value = tasksDoneData.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // Graph placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Graph visualization coming soon",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatisticsPeriod,
    onPeriodChange: (StatisticsPeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodButton(
            selected = selectedPeriod == StatisticsPeriod.DAY,
            onClick = { onPeriodChange(StatisticsPeriod.DAY) },
            icon = Icons.Default.CalendarToday,
            label = "Day"
        )
        PeriodButton(
            selected = selectedPeriod == StatisticsPeriod.WEEK,
            onClick = { onPeriodChange(StatisticsPeriod.WEEK) },
            icon = Icons.Default.CalendarViewWeek,
            label = "Week"
        )
        PeriodButton(
            selected = selectedPeriod == StatisticsPeriod.MONTH,
            onClick = { onPeriodChange(StatisticsPeriod.MONTH) },
            icon = Icons.Default.CalendarMonth,
            label = "Month"
        )
    }
}

@Composable
private fun PeriodButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
