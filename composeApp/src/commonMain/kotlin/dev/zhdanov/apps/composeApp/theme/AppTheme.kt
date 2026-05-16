package dev.zhdanov.apps.composeApp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightAppColorScheme = lightColorScheme(
//    primaryContainer = Color(0xFFEEF3F6),
    surfaceContainerHighest = Color(0xFFF8FAFB)
)

private val DarkAppColorScheme = darkColorScheme(
    surfaceContainerHighest = Color(0xFF2A3138)
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkAppColorScheme else LightAppColorScheme,
        content = content
    )
}
