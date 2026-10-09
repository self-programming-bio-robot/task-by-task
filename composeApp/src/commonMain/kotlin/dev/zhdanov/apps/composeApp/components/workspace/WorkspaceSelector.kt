package dev.zhdanov.apps.composeApp.components.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSelector(
    expandedContent: Boolean,
    modifier: Modifier = Modifier,
    workspaceSessionService: WorkspaceSessionService = koinInject()
) {
    val workspaces by workspaceSessionService.workspaces.collectAsState()
    val currentWorkspace by workspaceSessionService.currentWorkspace.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val currentIcon = workspaceIconVector(currentWorkspace?.icon)

    Column(
        modifier = if (expandedContent) modifier.padding(8.dp) else modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (expandedContent) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.widthIn(min = 160.dp, max = 240.dp)
            ) {
                Icon(currentIcon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    currentWorkspace?.name ?: "Workspace",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }
        } else {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Right),
                tooltip = { PlainTooltip { Text(currentWorkspace?.name ?: "Select workspace") } },
                state = rememberTooltipState(),
            ) {
                FilledTonalIconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = currentWorkspace?.name ?: "Select workspace",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            workspaces.forEach { workspace ->
                DropdownMenuItem(
                    text = {
                        Text(
                            workspace.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        workspaceSessionService.selectWorkspace(workspace.id)
                        menuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = workspaceIconVector(workspace.icon),
                            contentDescription = null
                        )
                    },
                    trailingIcon = if (workspace.id == currentWorkspace?.id) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null
                )
            }
            DropdownMenuItem(
                text = { Text("New workspace") },
                onClick = {
                    menuExpanded = false
                    showCreateDialog = true
                }
            )
        }
    }

    if (showCreateDialog) {
        CreateWorkspaceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                val workspace = workspaceSessionService.createWorkspace(name)
                workspaceSessionService.selectWorkspace(workspace.id)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateWorkspaceDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Workspace selection affects every screen in the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workspace name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
