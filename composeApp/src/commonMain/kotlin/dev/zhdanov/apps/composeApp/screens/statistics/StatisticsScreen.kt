package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
private val MIN_CHART_HEIGHT = 200.dp

// Approximate height of header elements (period selector + 2 stat card rows + spacing)
private val HEADER_HEIGHT = 200.dp

@OptIn(KoinExperimentalAPI::class)
@Composable
fun StatisticsScreen() {
    val viewModel: StatisticsViewModel = koinViewModel()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val periodOffset by viewModel.periodOffset.collectAsState()
    val focusTimeData by viewModel.focusTimeData.collectAsState()
    val workCyclesData by viewModel.workCyclesData.collectAsState()
    val tasksCreatedData by viewModel.tasksCreatedData.collectAsState()
    val tasksDoneData by viewModel.tasksDoneData.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    val density = LocalDensity.current
    var availableHeightDp by remember { mutableStateOf(0.dp) }
    val windowInfo = currentWindowAdaptiveInfo()

    val isCompact = !windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(600)
    val padding = if (isCompact) 0.dp else 16.dp
    val shape = if (isCompact)
        RectangleShape else MaterialTheme.shapes.medium

    // Calculate if we have enough space for the chart
    val hasSpaceForChart = availableHeightDp >= HEADER_HEIGHT + MIN_CHART_HEIGHT

    Scaffold(
        topBar = { TopBar("Statistics") }
    ) { paddings ->
        Box(Modifier.padding(paddings)) {
            Box(modifier = Modifier
                .padding(start = padding, end = padding, bottom = padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = shape
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .onSizeChanged { size ->
                                val heightDp = with(density) { size.height.toDp() }
                                availableHeightDp = heightDp
                            },
                        verticalArrangement = if (hasSpaceForChart) Arrangement.spacedBy(16.dp) else Arrangement.SpaceEvenly
                    ) {
                        // Period Selection
                        PeriodSelector(
                            selectedPeriod = selectedPeriod,
                            onPeriodChange = { viewModel.setPeriod(it) }
                        )

                        // Period Navigator
                        PeriodNavigator(
                            label = periodLabel,
                            isCurrentPeriod = periodOffset == 0,
                            onPrevious = { viewModel.goToPreviousPeriod() },
                            onNext = { viewModel.goToNextPeriod() },
                            onTodayClick = { viewModel.goToCurrentPeriod() }
                        )

                        // Statistics Cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (!hasSpaceForChart) Modifier.weight(1f) else Modifier),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        // Focus Time Chart - only show when height is sufficient
                        if (chartData.isNotEmpty() && hasSpaceForChart) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                // Calculate max columns based on available width at composition time
                                val availableWidth = maxWidth - 32.dp // Account for Card padding
                                val maxColumns = (availableWidth / MIN_COLUMN_WIDTH).toInt()
                                    .coerceIn(1, MAX_COLUMNS)

                                // Update column count on initial composition and when constraints change
                                LaunchedEffect(maxColumns) {
                                    viewModel.updateColumnCount(maxColumns)
                                }

                                Card(
                                    modifier = Modifier.fillMaxSize()
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
                        }
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

    val density = LocalDensity.current
    var columnHeight by remember { mutableStateOf(130.dp) }

    Row(
        modifier = modifier.fillMaxHeight()
            .onSizeChanged { size ->
                columnHeight = with(density) { size.height.toDp() - 20.dp }
            },
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
                                (entry.focusTimeMinutes.toFloat() / maxFocusTime * columnHeight.value).dp
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
    onPeriodChange: (StatisticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
private fun PeriodNavigator(
    label: String,
    isCurrentPeriod: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous period"
            )
        }

        // Period label (clickable to return to today)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrentPeriod) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier
                .clickable(enabled = !isCurrentPeriod) { onTodayClick() }
                .padding(horizontal = 8.dp)
        )

        // Next button
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next period"
            )
        }
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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
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
