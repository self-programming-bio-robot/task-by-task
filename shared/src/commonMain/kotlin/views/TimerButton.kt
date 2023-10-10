package views

import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.icerock.moko.mvvm.compose.getViewModel
import dev.icerock.moko.mvvm.compose.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.toDateTimePeriod
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import theme.timerRestState
import theme.timerWorkingState
import viewsModels.TimerCondition
import viewsModels.TimerViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun TimerButton(
    modifier: Modifier,
) {
    val timerViewModel = getKoin().get<TimerViewModel>()
    val uiState by timerViewModel.uiState.collectAsState()

    val color = if (uiState.condition == TimerCondition.WORKING
        || uiState.condition == TimerCondition.WAIT_WORK) {
        MaterialTheme.colors.timerWorkingState
    } else {
        MaterialTheme.colors.timerRestState
    }

    ExtendedFloatingActionButton(
        modifier = modifier,
        backgroundColor = color,
        onClick = {
            if (uiState.condition.isActive()) {
                timerViewModel.stop()
            } else {
                timerViewModel.start()
            }
        },
        icon = {
            if (uiState.condition.isActive()) {
                Icon(Icons.Filled.PlayArrow, "Start")
            } else {
                Icon(Icons.Filled.Stop, "Done")
            }
        },
        text = { Text(text = uiState.formattedString) },
    )
}