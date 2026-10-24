package dev.zhdanov.apps.composeApp.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zhdanov.apps.composeApp.services.CellStatus
import dev.zhdanov.apps.composeApp.services.RoutineMatrixCell
import dev.zhdanov.apps.composeApp.services.RoutinePeriod
import dev.zhdanov.apps.composeApp.services.RoutineReportRow
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun RoutinesReportScreen() {
    val viewModel: RoutinesReportViewModel = koinViewModel()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val periodOffset by viewModel.periodOffset.collectAsState()
    val periodLabel by viewModel.periodLabel.collectAsState()
    val reportRows by viewModel.reportRows.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag(UiTestTags.RoutinesReportScreen),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onSelectPeriod = { viewModel.setPeriod(it) }
        )

        PeriodNavigator(
            label = periodLabel,
            isCurrentPeriod = periodOffset == 0,
            onPrevious = { viewModel.goToPreviousPeriod() },
            onNext = { viewModel.goToNextPeriod() },
            onTodayClick = { viewModel.goToCurrentPeriod() }
        )

        if (reportRows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No routines created yet. Add routines in Tasks screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            RoutineMatrixGrid(reportRows = reportRows)
        }
    }
}

@Composable
internal fun PeriodSelector(
    selectedPeriod: RoutinePeriod,
    onSelectPeriod: (RoutinePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoutinePeriod.entries.forEach { period ->
            val icon = when (period) {
                RoutinePeriod.WEEK -> Icons.Default.CalendarViewWeek
                RoutinePeriod.TWO_WEEKS -> Icons.Default.DateRange
                RoutinePeriod.MONTH -> Icons.Default.CalendarMonth
            }
            PeriodChipButton(
                selected = selectedPeriod == period,
                onClick = { onSelectPeriod(period) },
                icon = icon,
                label = period.title,
                modifier = Modifier.testTag(UiTestTags.routinePeriodChip(period.name))
            )
        }
    }
}

@Composable
private fun PeriodChipButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
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
        },
        modifier = modifier
    )
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
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous period"
            )
        }

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

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next period"
            )
        }
    }
}

@Composable
internal fun RoutineMatrixGrid(
    reportRows: List<RoutineReportRow>
) {
    val horizontalScrollState = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val dates = reportRows.firstOrNull()?.cells?.map { it.date }.orEmpty()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Spacer(modifier = Modifier.width(160.dp))

                dates.forEach { date ->
                    DateHeaderCell(date = date)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            reportRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(horizontalScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = row.routine.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (row.currentStreak > 0) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${row.currentStreak} day streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "${(row.completionRate * 100).toInt()}% completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    row.cells.forEach { cell ->
                        MatrixSquareCell(
                            routineId = row.routine.id,
                            cell = cell
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeaderCell(date: LocalDate) {
    val dayOfWeekStr = date.dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercase() }
    val dayOfMonthStr = date.day.toString()

    Column(
        modifier = Modifier
            .width(36.dp)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayOfWeekStr,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = dayOfMonthStr,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MatrixSquareCell(
    routineId: Long,
    cell: RoutineMatrixCell
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceContainerHighest

    val shape = RoundedCornerShape(6.dp)

    val (bgColor, borderColor, iconColor) = when (cell.status) {
        CellStatus.COMPLETED -> Triple(primaryColor, primaryColor, MaterialTheme.colorScheme.onPrimary)
        CellStatus.MISSED -> Triple(errorColor.copy(alpha = 0.15f), errorColor.copy(alpha = 0.3f), errorColor)
        CellStatus.PENDING -> Triple(Color.Transparent, primaryColor, primaryColor)
        CellStatus.INACTIVE -> Triple(surfaceVariant.copy(alpha = 0.3f), Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(32.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .testTag(UiTestTags.routineMatrixCell(routineId, cell.date.toLong())),
        contentAlignment = Alignment.Center
    ) {
        if (cell.status == CellStatus.COMPLETED) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
