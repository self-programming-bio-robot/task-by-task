package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.workspace.WorkspaceIconOptions
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalComposeUiApi::class)
@Composable
fun GeneralSettings() {
    val viewModel: GeneralSettingsViewModel = koinViewModel()
    val theme by viewModel.theme.collectAsState()
    val startOfDay by viewModel.startOfDay.collectAsState()
    val workspaceName by viewModel.workspaceName.collectAsState()
    val workspaceIcon by viewModel.workspaceIcon.collectAsState()

    var hourInput by remember { mutableStateOf(startOfDay.hour.toString().padStart(2, '0')) }
    var minuteInput by remember { mutableStateOf(startOfDay.minute.toString().padStart(2, '0')) }
    val iconScrollState = rememberScrollState()

    LaunchedEffect(startOfDay) {
        hourInput = startOfDay.hour.toString().padStart(2, '0')
        minuteInput = startOfDay.minute.toString().padStart(2, '0')
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Workspace", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = workspaceName,
            onValueChange = { viewModel.updateWorkspaceName(it) },
            label = { Text("Workspace name") },
            singleLine = true,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
        Text(
            "Icon",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .horizontalScroll(iconScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkspaceIconOptions.forEach { option ->
                val selected = workspaceIcon == option.id
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.updateWorkspaceIcon(option.id) },
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = if (selected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.imageVector,
                            contentDescription = option.title
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = theme == "auto",
                    onClick = { viewModel.updateTheme("auto") }
                )
                Text("Auto", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = theme == "light",
                    onClick = { viewModel.updateTheme("light") }
                )
                Text("Light", modifier = Modifier.padding(start = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = theme == "dark",
                    onClick = { viewModel.updateTheme("dark") }
                )
                Text("Dark", modifier = Modifier.padding(start = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Start of Day", style = MaterialTheme.typography.titleMedium)
        Text(
            "The time when a new day begins (affects day summaries and statistics)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hourInput,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    hourInput = filtered
                    val hour = filtered.toIntOrNull()
                    if (hour != null && hour in 0..23) {
                        val minute = minuteInput.toIntOrNull() ?: 0
                        viewModel.updateStartOfDay(LocalTime(hour, minute.coerceIn(0, 59)))
                    }
                },
                label = { Text("Hour") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            OutlinedTextField(
                value = minuteInput,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }.take(2)
                    minuteInput = filtered
                    val minute = filtered.toIntOrNull()
                    if (minute != null && minute in 0..59) {
                        val hour = hourInput.toIntOrNull() ?: 0
                        viewModel.updateStartOfDay(LocalTime(hour.coerceIn(0, 23), minute))
                    }
                },
                label = { Text("Minute") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }
    }
}
