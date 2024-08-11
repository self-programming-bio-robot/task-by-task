package dev.zhdanov.apps.composeApp.screens.feedback

import androidx.compose.runtime.Composable
import dev.zhdanov.apps.shared.cache.focus.CreateFocusTime

@Composable
expect fun Feedback(
    finishAt: Long,
    duration: Int,
    onExit: (feedback: CreateFocusTime?) -> Unit
)