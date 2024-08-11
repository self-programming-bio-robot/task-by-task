package dev.zhdanov.apps.composeApp.screens.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import dev.zhdanov.apps.shared.cache.focus.CreateFocusTime

@Composable
actual fun Feedback(
    finishAt: Long,
    duration: Int,
    onExit: (feedback: CreateFocusTime?) -> Unit
) {
    var text = remember { mutableStateOf("") }

    Window(
        onCloseRequest = {
            onExit(null)
        },
        title = "Feedback",
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    label = { Text("Enter your feedback") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onExit(CreateFocusTime(duration, text.value, finishAt)) }) {
                    Text("Save")
                }
            }
        }
    }
}