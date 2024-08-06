package dev.zhdanov.apps.composeApp.components.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.shared.TimerState
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

val LightBlue = Color(0xFF87CEFA)
val LightGreen = Color(0xFF32CD32)
val DarkGreen = Color(0xFF006400)
val LightGray = Color(0xFFD3D3D3)
val DarkGray = Color(0xFFA9A9A9)
val Yellow = Color(0xFFFFD700)
val Teal = Color(0xFF20B2AA)
val Purple = Color(0xFF800080)
val DisabledBackground = Color(0xFFD3D3D3) // Light Gray for disabled background
val DisabledContent = Color(0xFFA9A9A9)   // Dark Gray for disabled content

private val WORK_SHAPE_COLOR = Color(red = 0x87, green = 0xCE, blue = 0xFA)


@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun TimerView() {
    val viewModel = koinViewModel<TimerViewModel>()
    val isPause = viewModel.isPause.collectAsState()
    val isRunning = viewModel.isRunning.collectAsState()
    val time = viewModel.time.collectAsState("")
    val state = viewModel.state.collectAsState()

    val buttonStateColors = if (state.value == TimerState.WORK)
        buttonColors(
            backgroundColor = LightGreen,
            contentColor = Color.White,
            disabledBackgroundColor = DisabledBackground,
            disabledContentColor = DisabledContent
        )
    else
        buttonColors(
            backgroundColor = DarkGreen,
            contentColor = Color.White,
            disabledBackgroundColor = LightGray,
            disabledContentColor = DarkGray,
        )

    Box(
        modifier = Modifier
            .background(
                color = if (state.value == TimerState.WORK) LightBlue else LightGreen,
                shape = CircleShape
            )
            .size(200.dp),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                if (!isRunning.value) {
                    Button(
                        colors = buttonStateColors,
                        enabled = !isRunning.value,
                        onClick = { viewModel.startTimer() }
                    ) {
                        Text("Start")
                    }
                } else {
                    Button(
                        colors = buttonStateColors,
                        enabled = isRunning.value,
                        onClick = {
                            viewModel.pauseTimer()
                        }
                    ) {
                        if (isPause.value) {
                            Text("Resume")
                        } else {
                            Text("Pause")
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.value == TimerState.WORK) {
                        Button(
                            colors = buttonStateColors,
                            enabled = isRunning.value,
                            onClick = { viewModel.stopTimer() }
                        ) {
                            Text("Stop")
                        }
                    } else {
                        Button(
                            colors = buttonStateColors,
                            enabled = isRunning.value,
                            onClick = { viewModel.skipTimer() }
                        ) {
                            Text("Skip")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(time.value, textAlign = TextAlign.Center)
        }
    }
}