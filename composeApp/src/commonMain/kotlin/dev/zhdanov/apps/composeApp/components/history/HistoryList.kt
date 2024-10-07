package dev.zhdanov.apps.composeApp.components.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import dev.zhdanov.apps.shared.model.DaySummary
import kotlin.time.Duration.Companion.seconds

@Composable
fun HistoryList(items: List<DaySummary>) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No items available", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            // todo: add scroll
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.date }) { item ->
                ListItemView(item)
            }
        }
    }
}

@Composable
fun ListItemView(item: DaySummary) {
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

        Markdown(item.review)
    }
}
