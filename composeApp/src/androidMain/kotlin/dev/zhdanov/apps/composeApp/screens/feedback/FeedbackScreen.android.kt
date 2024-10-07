package dev.zhdanov.apps.composeApp.screens.feedback

import androidx.compose.runtime.Composable
import dev.zhdanov.apps.shared.model.CreateFocusTime

@Composable
actual fun Feedback(finishAt: Long, duration: Int, onExit: (feedback: CreateFocusTime?) -> Unit) {
}
