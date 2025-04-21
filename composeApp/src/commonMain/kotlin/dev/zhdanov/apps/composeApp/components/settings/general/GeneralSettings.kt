package dev.zhdanov.apps.composeApp.components.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun GeneralSettings() {
    val viewModel: GeneralSettingsViewModel = koinViewModel()
    val openAiToken by viewModel.openAiToken.collectAsState()
    val (passwordVisible, setPasswordVisible) = remember { mutableStateOf(false) }

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
    }
}
