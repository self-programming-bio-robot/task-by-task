package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.AppPane
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.model.Routine
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun TaskRoutinesPane(
    onRoutineClick: (Long) -> Unit = {},
) {
    val viewModel: TaskRoutinesViewModel = koinViewModel<TaskRoutinesViewModel>()
    val routines by viewModel.routines.collectAsState()

    AppPane(
        title = Screen.TaskRoutines.title,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.testTag(UiTestTags.TaskRoutinesScreen),
    ) {
        RoutineList(
            routines = routines,
            onAddRoutine = { title -> viewModel.addNewRoutine(title) },
            onRoutineClick = { onRoutineClick(it.id) },
            onToggleActive = { viewModel.toggleRoutineActive(it.id) },
            onDelete = { viewModel.deleteRoutine(it.id) }
        )
    }
}

@Composable
fun RoutineList(
    routines: List<Routine>,
    onAddRoutine: (String) -> Unit,
    onRoutineClick: (Routine) -> Unit = {},
    onToggleActive: (Routine) -> Unit,
    onDelete: (Routine) -> Unit,
) {
    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                NewRoutineInput(onAddRoutine = onAddRoutine)
            }
            if (routines.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        text = "No routines yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(routines, key = { it.id }) { routine ->
                    RoutineItem(
                        routine = routine,
                        onClick = { onRoutineClick(routine) },
                        onToggleActive = { onToggleActive(routine) },
                        onDelete = { onDelete(routine) }
                    )
                }
            }
        }
    }
}

@Composable
fun NewRoutineInput(
    onAddRoutine: (value: String) -> Unit
) {
    var value by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .weight(1f)
                .testTag(UiTestTags.NewRoutineInput),
            placeholder = { Text("Enter new daily routine") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(
            onClick = {
                val title = value.trim()
                if (title.isNotEmpty()) {
                    onAddRoutine(title)
                }
                value = ""
            },
            modifier = Modifier.testTag(UiTestTags.AddRoutineButton)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add routine")
        }
    }
}

@Composable
fun RoutineItem(
    routine: Routine,
    onClick: () -> Unit = {},
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.routineRow(routine.id))
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = "Daily Routine",
            tint = if (routine.isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = routine.title,
                style = MaterialTheme.typography.bodyLarge
            )
            val desc = routine.description
            if (!desc.isNullOrBlank()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = routine.isActive,
            onCheckedChange = { onToggleActive() },
            modifier = Modifier.testTag(UiTestTags.routineActiveToggle(routine.id))
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag(UiTestTags.routineDeleteButton(routine.id))
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete routine",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Extra pane that edits a single routine template identified by [routineId].
 */
@OptIn(KoinExperimentalAPI::class)
@Composable
fun RoutineEditPane(
    routineId: Long,
    onDone: () -> Unit,
) {
    val viewModel: TaskRoutinesViewModel = koinViewModel<TaskRoutinesViewModel>()
    val routines by viewModel.routines.collectAsState()
    val routine = routines.find { it.id == routineId }
    val coroutineScope = rememberCoroutineScope()

    AppPane(
        title = "Edit routine",
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        if (routine == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Routine not found")
            }
        } else {
            key(routineId) {
                RoutineDetails(
                    routine = routine,
                    onUpdatedRoutine = { updatedRoutine ->
                        coroutineScope.launch {
                            viewModel.updateRoutine(updatedRoutine)
                            onDone()
                        }
                    },
                    onDeleteRoutine = {
                        coroutineScope.launch {
                            viewModel.deleteRoutine(routine.id)
                            onDone()
                        }
                    },
                    onCancel = onDone
                )
            }
        }
    }
}

@Composable
fun RoutineDetails(
    routine: Routine,
    onUpdatedRoutine: (routine: Routine) -> Unit,
    onDeleteRoutine: () -> Unit = {},
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(routine.title) }
    var description by remember { mutableStateOf(routine.description.orEmpty()) }
    var isActive by remember { mutableStateOf(routine.isActive) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Active")
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDeleteRoutine,
                modifier = Modifier.testTag(UiTestTags.RoutineDeleteInEditorButton)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete routine",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        title = routine.title
                        description = routine.description.orEmpty()
                        isActive = routine.isActive
                        onCancel()
                    }
                ) {
                    Text("Cancel", maxLines = 1, softWrap = false)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onUpdatedRoutine(
                            routine.copy(
                                title = title.trim(),
                                description = description.trim().takeIf { it.isNotBlank() },
                                isActive = isActive
                            )
                        )
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
