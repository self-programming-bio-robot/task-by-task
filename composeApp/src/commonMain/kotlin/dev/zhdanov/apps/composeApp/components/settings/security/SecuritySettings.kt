package dev.zhdanov.apps.composeApp.components.settings.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun SecuritySettings() {
    val viewModel: SecuritySettingsViewModel = koinViewModel()
    val openAiToken by viewModel.openAiToken.collectAsState()
    val llmBaseUrl by viewModel.llmBaseUrl.collectAsState()
    val llmModelId by viewModel.llmModelId.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val securitySettings by viewModel.securitySettings.collectAsState()
    val isWorkspaceLocked by viewModel.isCurrentWorkspaceLocked.collectAsState()
    val securityError by viewModel.securityError.collectAsState()

    val (passwordVisible, setPasswordVisible) = remember { mutableStateOf(false) }
    val (pin, setPin) = remember { mutableStateOf("") }
    val (pinConfirm, setPinConfirm) = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Security", style = MaterialTheme.typography.titleMedium)
        Text(
            currentWorkspace?.name.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (securityError != null) {
            Text(
                securityError.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Text(
            if (securitySettings?.encryptionEnabled == true) {
                if (isWorkspaceLocked) "Encrypted and locked" else "Encrypted and unlocked"
            } else {
                "Not encrypted"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = llmBaseUrl,
            onValueChange = { viewModel.updateBaseUrl(it) },
            label = { Text("OpenAI-compatible base URL") },
            singleLine = true,
            enabled = !isWorkspaceLocked,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        )

        OutlinedTextField(
            value = llmModelId,
            onValueChange = { viewModel.updateModelId(it) },
            label = { Text("Model") },
            singleLine = true,
            enabled = !isWorkspaceLocked,
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
        )

        OutlinedTextField(
            value = openAiToken,
            onValueChange = { viewModel.updateToken(it) },
            label = { Text("API token") },
            singleLine = true,
            enabled = !isWorkspaceLocked,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                val description = if (passwordVisible) "Hide" else "Show"
                IconButton(onClick = { setPasswordVisible(!passwordVisible) }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text("Local encryption", style = MaterialTheme.typography.titleSmall)
        Text(
            "Workspace data is encrypted locally with a key unlocked by this PIN.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = pin,
            onValueChange = setPin,
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
        )

        if (securitySettings?.encryptionEnabled != true) {
            OutlinedTextField(
                value = pinConfirm,
                onValueChange = setPinConfirm,
                label = { Text("Confirm PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                securitySettings?.encryptionEnabled != true -> {
                    Button(
                        onClick = {
                            viewModel.enableEncryption(pin, pinConfirm)
                            setPin("")
                            setPinConfirm("")
                        },
                        enabled = pin.isNotBlank() && pinConfirm.isNotBlank()
                    ) {
                        Text("Encrypt")
                    }
                }
                isWorkspaceLocked -> {
                    Button(
                        onClick = {
                            viewModel.unlockWorkspace(pin)
                            setPin("")
                        },
                        enabled = pin.isNotBlank()
                    ) {
                        Text("Unlock")
                    }
                }
                else -> {
                    Button(onClick = { viewModel.lockWorkspace() }) {
                        Text("Lock")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.disableEncryption(pin)
                            setPin("")
                        },
                        enabled = pin.isNotBlank()
                    ) {
                        Text("Disable encryption")
                    }
                }
            }
        }
    }
}
