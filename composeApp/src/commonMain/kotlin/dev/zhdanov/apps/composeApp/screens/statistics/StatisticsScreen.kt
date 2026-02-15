package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.math.max

enum class StatisticsPeriod {
    DAY,
    WEEK,
    MONTH
}

// Minimum width per bar column in dp
private val MIN_COLUMN_WIDTH = 40.dp

// Maximum columns to show regardless of width
private val MAX_COLUMNS = 24

// Minimum height to show chart in dp
private val MIN_CHART_HEIGHT = 400.dp

@OptIn(KoinExperimentalAPI::class)
@Composable
fun StatisticsScreen() {
    val viewModel: StatisticsViewModel = koinViewModel()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val focusTimeData by viewModel.focusTimeData.collectAsState()
    val workCyclesData by viewModel.workCyclesData.collectAsState()
    val tasksCreatedData by viewModel.tasksCreatedData.collectAsState()
    val tasksDoneData by viewModel.tasksDoneData.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    val density = LocalDensity.current
    var showChart by remember { mutableStateOf(true) }

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

            // Focus Time Chart
            if (chartData.isNotEmpty() && showChart) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onSizeChanged { size ->
                            val widthDp = with(density) { size.width.toDp() }
                            val heightDp = with(density) { size.height.toDp() }

                            // Check if height is sufficient for chart
                            showChart = heightDp >= MIN_CHART_HEIGHT

                            if (showChart) {
                                // Calculate max columns based on available width
                                // Account for padding (16dp * 2 = 32dp)
                                val availableWidth = widthDp - 32.dp
                                val maxColumns = (availableWidth / MIN_COLUMN_WIDTH).toInt()
                                    .coerceIn(1, MAX_COLUMNS)
                                viewModel.updateColumnCount(maxColumns)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Focus Time (minutes)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SimpleBarChart(
                            data = chartData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            // Show message when height is too small
            if (!showChart) {
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
                            text = "Increase window height to view chart",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleBarChart(
    data: List<ChartEntry>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No data available")
        }
        return
    }

    val maxFocusTime = max(1, data.maxOf { it.focusTimeMinutes })

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        data.forEach { entry ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (entry.focusTimeMinutes > 0) {
                                (entry.focusTimeMinutes.toFloat() / maxFocusTime * 150).dp
                            } else {
                                4.dp
                            }
                        )
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Label
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
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
