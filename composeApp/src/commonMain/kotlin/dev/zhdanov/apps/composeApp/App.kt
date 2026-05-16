package dev.zhdanov.apps.composeApp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.layout.AdaptiveLayout
import dev.zhdanov.apps.composeApp.services.AppSettingsService
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.ThemeChangeService
import dev.zhdanov.apps.composeApp.services.ThemeChangeService.SystemTheme
import dev.zhdanov.apps.composeApp.services.TimerSettingsService
import dev.zhdanov.apps.composeApp.services.WorkspaceSessionService
import dev.zhdanov.apps.composeApp.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    val daySummaryService = koinInject<DaySummaryService>()
    val timerSettingsService = koinInject<TimerSettingsService>()
    val workspaceSessionService = koinInject<WorkspaceSessionService>()
    val appSettingsService = koinInject<AppSettingsService>()

    val theme by appSettingsService.theme.collectAsState()
    val isWorkspaceLocked by workspaceSessionService.isCurrentWorkspaceLocked.collectAsState()
    val systemTheme = rememberSystemTheme()

    val useDarkTheme = when (theme) {
        "dark" -> true
        "light" -> false
        else -> systemTheme == SystemTheme.DARK
    }

    LaunchedEffect(isWorkspaceLocked) {
        if (!isWorkspaceLocked) {
            daySummaryService.migration()
            timerSettingsService.migration()
            appSettingsService.loadTheme()
        }
    }
    AppTheme(useDarkTheme = useDarkTheme) {
        KoinContext {
            if (isWorkspaceLocked) {
                WorkspaceUnlockGate(workspaceSessionService)
            } else {
                AdaptiveLayout()
            }
        }
    }
}

@Composable
private fun WorkspaceUnlockGate(workspaceSessionService: WorkspaceSessionService) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Workspace locked", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter the workspace PIN to unlock local encrypted data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (error != null) {
            Text(
                error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(0.4f)
        )
        Button(
            onClick = {
                runCatching {
                    workspaceSessionService.unlockCurrentWorkspace(pin)
                }.onSuccess {
                    pin = ""
                    error = null
                }.onFailure {
                    error = "Invalid PIN"
                }
            },
            enabled = pin.isNotBlank(),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Unlock")
        }
    }
}

@Composable
fun rememberSystemTheme(): SystemTheme {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = remember { mutableStateOf(if (isSystemInDarkTheme) SystemTheme.DARK else SystemTheme.LIGHT ) }
    val themeChangeService = koinInject<ThemeChangeService>()

    DisposableEffect(Unit) {
        val listener = ThemeChangeService.ThemeChangeListener { theme: SystemTheme ->
            isDarkTheme.value = theme
        }
        themeChangeService.registerListener(listener)
        onDispose {
            themeChangeService.removeListener(listener)
        }
    }

    return isDarkTheme.value
}
