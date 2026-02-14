package dev.zhdanov.apps.composeApp.components.settings.timers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.shared.model.TimerSettings
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import kotlin.time.Duration.Companion.seconds

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3Api::class)
@Composable
fun TimersSettings(
    onItemClick: (item: TimerSettings) -> Unit,
    onCreate: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinViewModel<TimersSettingsViewModel>()
    val timerSettings by viewModel.timerSettings.collectAsState(emptyList())
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onCreate) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Timer"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(timerSettings) { item ->
                    TimerSettingsCompactView(
                        item,
                        onClick = { onItemClick(item) },
                        onDelete = { viewModel.removeSetting(it.id) },
                        onSetDefault = { viewModel.setDefault(it.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimerSettingsCompactView(
    settings: TimerSettings,
    onClick: () -> Unit,
    onDelete: (timerSetting: TimerSettings) -> Unit,
    onSetDefault: (timerSetting: TimerSettings) -> Unit,
) {
    val isDefault = settings.default

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Work Duration Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Work", style = MaterialTheme.typography.bodySmall)
                Text(text = "${settings.workDuration.seconds}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.width(16.dp))

            // Short Break Duration Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Short Break", style = MaterialTheme.typography.bodySmall)
                Text(text = "${settings.shortBreakDuration.seconds}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.width(16.dp))

            // Long Break Duration Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Long Break", style = MaterialTheme.typography.bodySmall)
                Text(text = "${settings.longBreakDuration.seconds}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.width(16.dp))

            // Work Cycles Display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Cycles", style = MaterialTheme.typography.bodySmall)
                Text(text = "${settings.workCycles}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.width(16.dp))

            IconButton(
                onClick = { onSetDefault(settings) },
                modifier = Modifier,
                enabled = !isDefault
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Set favorite",
                    tint = if (!isDefault) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.tertiary
                )
            }
            IconButton(
                onClick = { onDelete(settings) },
                modifier = Modifier,
                enabled = !isDefault
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove",
                    tint = if (isDefault) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
