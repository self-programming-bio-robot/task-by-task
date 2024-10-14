package dev.zhdanov.apps.composeApp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.timer.TimerView
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun HomeScreen(
    navigateToHistory: () -> Unit,
    navigateToSettings: () -> Unit,
    onFinishDay: (review: AssistantReviewResponse) -> Unit
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val isActive by viewModel.isActive.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        TimerView()

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { navigateToHistory() }
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryEdu,
                    contentDescription = "History",
                    tint = Color.Black
                )
            }

            IconButton(
                onClick = { navigateToSettings() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black
                )
            }
        }


        Button(
            enabled = isActive,
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    onFinishDay(viewModel.finishDay())
                    isLoading = false
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Text("Finish day")
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { }
                    )
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    }
}
