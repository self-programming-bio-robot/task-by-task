package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun GeneralSettings() {
    val viewModel: GeneralSettingsViewModel = koinViewModel()
    val openAiToken by viewModel.openAiToken.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val startOfDay by viewModel.startOfDay.collectAsState()
    val (passwordVisible, setPasswordVisible) = remember { mutableStateOf(false) }

    var hourInput by remember { mutableStateOf(startOfDay.hour.toString().padStart(2, '0')) }
    var minuteInput by remember { mutableStateOf(startOfDay.minute.toString().padStart(2, '0')) }

    // Update inputs when startOfDay changes from DB
    LaunchedEffect(startOfDay) {
        hourInput = startOfDay.hour.toString().padStart(2, '0')
        minuteInput = startOfDay.minute.toString().padStart(2, '0')
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = openAiToken,
            onValueChange = { viewModel.updateToken(it) },
            label = { Text("OpenAI Token") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                val description = if (passwordVisible) "Hide" else "Show"
                IconButton(onClick = { setPasswordVisible(!passwordVisible) }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Theme")
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
        Text("Start of Day", style = MaterialTheme.typography.bodyLarge)
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
