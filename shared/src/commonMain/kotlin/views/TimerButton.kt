package views

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import org.koin.compose.getKoin
import viewsModels.TimerCondition
import viewsModels.TimerViewModel

@Composable
fun TimerButton(
    modifier: Modifier,
) {
    val timerViewModel = getKoin().get<TimerViewModel>()
    val uiState by timerViewModel.uiState.collectAsState()

    val color = if (uiState.condition == TimerCondition.WORKING
        || uiState.condition == TimerCondition.WAIT_WORK) {
        AppTheme.pomodoroColors.workState
    } else {
        AppTheme.pomodoroColors.restState
    }

    ExtendedFloatingActionButton(
        modifier = modifier,
        containerColor = color,
        onClick = {
            if (uiState.condition.isActive()) {
                timerViewModel.stop()
            } else {
                timerViewModel.start()
            }
        },
        icon = {
            if (!uiState.condition.isActive()) {
                Icon(Icons.Filled.PlayArrow, "Start")
            } else {
                Icon(Icons.Filled.Stop, "Done")
            }
        },
        text = { Text(text = uiState.formattedString) },
    )
}