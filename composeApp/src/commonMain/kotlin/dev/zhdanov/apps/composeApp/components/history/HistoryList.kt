package dev.zhdanov.apps.composeApp.components.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.TaskSummary
import kotlin.time.Duration.Companion.seconds

@Composable
fun HistoryList(
    items: List<DaySummary>,
    onDayClick: (DaySummary) -> Unit = {}
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No items available", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.date }) { item ->
                ListItemView(item, onClick = { onDayClick(item) })
            }
        }
    }
}

@Composable
fun ListItemView(
    item: DaySummary,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
        // Header (Date and Focus Time)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.date.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Focus Time: ${item.focusTime.seconds}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Linked Tasks Display
        if (item.linkedTasks.isNotEmpty()) {
            LinkedTasksSection(tasks = item.linkedTasks)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Markdown(item.review)
        }
    }
}

/**
 * UI Component: LinkedTaskChip
 * Location: In each ListItemView, between header and review
 * Shows: Task icon + task title
 * Background: MaterialTheme.colorScheme.secondaryContainer
 * Border: outlineVariant
 */
@Composable
private fun LinkedTasksSection(tasks: List<TaskSummary>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tasks.forEach { task ->
            LinkedTaskChip(
                title = task.title,
                totalDuration = task.totalDuration
            )
        }
    }
}

@Composable
private fun LinkedTaskChip(
    title: String,
    totalDuration: Long
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Linked task",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${totalDuration / 60}min",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
