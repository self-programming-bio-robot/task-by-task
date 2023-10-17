package views.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.example.compose.AppTheme
import kotlinx.coroutines.launch
import models.TodoItem
import org.koin.compose.getKoin
import services.TodoService
import views.CheckCircle
import views.Pomodoro
import views.PomodoroSize
import viewsModels.TimerCondition
import viewsModels.TimerState
import viewsModels.TimerViewModel
import kotlin.math.abs

internal object TimerTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Filled.AvTimer)
            return remember {
                TabOptions(
                    index = 0u,
                    title = "Timer",
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        Navigator(TimerScreen())
    }
}

class TimerScreen : Screen {
    @Composable
    @ExperimentalMaterial3Api
    override fun Content() {
        val timerViewModel = getKoin().get<TimerViewModel>()
        val todoService = getKoin().get<TodoService>()
        val uiState by timerViewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val color = if (uiState.condition == TimerCondition.WORKING
                    || uiState.condition == TimerCondition.WAIT_WORK
                ) {
                    AppTheme.pomodoroColors.workState
                } else {
                    AppTheme.pomodoroColors.restState
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .aspectRatio(1f),
                    shape = CircleShape,
                    color = color,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = uiState.formattedString,
                            textAlign = TextAlign.Center,
                            fontSize = 64.sp
                        )
                        TimerControls(uiState, timerViewModel)
                    }
                }
            }
            Row(
                modifier = Modifier,
            ) {
                TaskCard(
                    todo = uiState.focusTodo,
                    timerState = uiState,
                    onPick = {
                        navigator.push(TodoScreen {
                            timerViewModel.setFocusTodo(it)
                        })
                    },
                    onDone = {
                        todoService.switch(it.id, true)
                        timerViewModel.clearFocusTodo()
                    },
                    onCancel = {
                        timerViewModel.clearFocusTodo()
                    }
                )
            }
        }
    }

    @Composable
    private fun TimerControls(uiState: TimerState, timerViewModel: TimerViewModel) {
        Row {
            when (uiState.condition) {
                TimerCondition.WAIT_WORK,
                TimerCondition.WAIT_LONG_REST,
                TimerCondition.WAIT_SHORT_REST -> {
                    IconButton(onClick = {
                        timerViewModel.start()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                else -> {
                    IconButton(onClick = {
                        timerViewModel.stop()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

        }
    }
}

private enum class TaskSwipeState {
    NORMAL,
    LEFT,
    RIGHT
}

@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    timerState: TimerState,
    todo: TodoItem? = null,
    onPick: () -> Unit,
    onDone: (todo: TodoItem) -> Unit,
    onCancel: () -> Unit
) {

    val coroutineScope = rememberCoroutineScope()
    val indicationSource = remember { MutableInteractionSource() }
    val maxOffsetPx = with(LocalDensity.current) { 108.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val backgroundColor: Color by animateColorAsState(
        if (abs(offsetX.value) < maxOffsetPx) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.errorContainer
    )
    val swipeableState = remember {
        DraggableState {
            coroutineScope.launch {
                offsetX.snapTo(offsetX.value + it)
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(8.dp)
            .indication(indicationSource, rememberRipple())
            .draggable(
                state = swipeableState,
                enabled = todo != null,
                orientation = Orientation.Horizontal,
                interactionSource = interactionSource,
                onDragStopped = {
                    coroutineScope.launch {
                        if (abs(offsetX.value) >= maxOffsetPx) {
                            onCancel()
                        }
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = 0
                            )
                        )
                    }
                }
            )
            .offset { IntOffset(x = offsetX.value.toInt(), y = 0) }
            .then(modifier),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        todo?.let { todo ->
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (timerState.condition == TimerCondition.WORKING) {
                        Pomodoro(size = PomodoroSize.LARGE)
                    }
                    todo.pomodoros.asSequence().forEach { _ ->
                        Pomodoro()
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row {
                            Text(
                                text = todo.title,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        if (todo.description.isNotBlank()) {
                            Row {
                                Text(text = todo.description)
                            }
                        }
                    }

                    CheckCircle(
                        checked = false,
                        onCheckedChange = {
                            onDone(todo)
                        },
                        interactionSource = indicationSource
                    )
                }
            }
        } ?: run {
            TextButton(modifier = Modifier.fillMaxWidth(), onClick = onPick) {
                Text("Pick your focus",
                    color = contentColorFor(backgroundColor))
            }
        }
    }
}