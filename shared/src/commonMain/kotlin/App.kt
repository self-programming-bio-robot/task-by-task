import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.mvvm.compose.getViewModel
import dev.icerock.moko.mvvm.compose.viewModelFactory
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toDateTimePeriod
import kotlinx.datetime.toLocalDateTime
import models.TodoItem
import org.jetbrains.compose.resources.ExperimentalResourceApi
import services.TodoRepository
import services.TodoService
import viewsModels.TimerCondition
import viewsModels.TimerViewModel
import viewsModels.TodoListViewModel
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    val timerViewModel = getViewModel(Unit, viewModelFactory {
        TimerViewModel()
    })

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            TodayTab()
            TimerButton(
                timerViewModel,
                Modifier
                    .padding(all = 16.dp)
                    .padding(bottom = 48.dp)
                    .align(alignment = Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun TimerButton(
    timerViewModel: TimerViewModel,
    modifier: Modifier,
) {
    val uiState by timerViewModel.uiState.collectAsState()
    val time = uiState.reminders.toDateTimePeriod()
    val minutes = if (time.minutes < 10) {
        "0${time.minutes}"
    } else {
        time.minutes.toString()
    }
    val seconds = if (time.seconds < 10) {
        "0${time.seconds}"
    } else {
        time.seconds.toString()
    }

    LaunchedEffect(uiState.started) {
        var done = uiState.started == null;
        while (!done) {
            done = timerViewModel.updateTime(Clock.System.now())
            delay(1.seconds)
        }
    }

    ExtendedFloatingActionButton(
        modifier = modifier,
        onClick = { timerViewModel.start() },
        icon = {
            when (uiState.condition) {
                TimerCondition.WAIT_LONG_REST, TimerCondition.WAIT_SHORT_REST, TimerCondition.WAIT_WORK ->
                    Icon(Icons.Filled.PlayArrow, "Start")

                else -> Icon(Icons.Filled.Stop, "Done")
            }
        },
        text = { Text(text = "$minutes:$seconds") },
    )
}

@Composable
fun TodayTab() {
    var todoService by remember { mutableStateOf(TodoService(TodoRepository())) }
    var todoListViewModel =
        getViewModel(Unit, viewModelFactory { TodoListViewModel(todoService) })

    Column {
        DayHeader(
            date = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
        TodoList(todoListViewModel, Modifier.weight(1f))
        NewTaskInput {
            todoListViewModel.addTodo(it)
        }
    }
}

@Composable
fun DayHeader(
    modifier: Modifier = Modifier,
    date: LocalDate
) {
    val day = date.dayOfMonth
    val month = date.month.name.substring(0..2)
    val year = date.year
    val dayOfWeek = date.dayOfWeek.name

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp)
            .then(modifier)
    ) {
        Text(
            text = day.toString(),
            fontSize = 64.sp,
            fontWeight = FontWeight.Medium
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                month.uppercase(),
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp
            )
            Text(year.toString())
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = dayOfWeek.uppercase(),
            fontSize = 32.sp
        )
    }
}

@Composable
fun TodoList(
    todoListViewModel: TodoListViewModel,
    modifier: Modifier,
) {
    val uiState by todoListViewModel.uiState.collectAsState()
    Column(
        modifier = Modifier.padding(16.dp)
            .verticalScroll(rememberScrollState())
            .then(modifier)
    ) {
        if (uiState.list.isEmpty()) {
            Text("All is done!")
        } else {
            uiState.list.forEach {
                TodoItem(it) { newState ->
                    todoListViewModel.switchTodo(it.id, newState)
                }
            }
            Row(Modifier.height(48.dp)) {}
        }
    }
}

@Composable
fun NewTaskInput(onEnter: (text: String) -> Unit) {
    var newTodoTitle by remember { mutableStateOf("") }

    fun send() {
        onEnter(newTodoTitle)
        newTodoTitle = ""
    }

    Row {
        TextField(newTodoTitle,
            modifier = Modifier.weight(1.0f),
            singleLine = true,
            placeholder = @Composable {
                Text("Enter you task")
            },
            onValueChange = {
                newTodoTitle = it
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                send()
            }),
            trailingIcon = @Composable {
                IconButton(onClick = {
                    send()
                }) {
                    Icon(Icons.Rounded.Send, "send")
                }
            }
        )
    }
}

@Composable
fun TodoItem(item: TodoItem, onChange: (state: Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = item.title.replaceFirstChar { it.titlecase() },
                fontSize = 20.sp,
                color = if (item.done) Color.LightGray else Color.DarkGray
            )
            if (item.pomodoros.isNotEmpty()) {
                Row {
                    item.pomodoros.forEach {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        CheckCircle(
            checked = item.done,
            onCheckedChange = onChange
        )
    }
}


@Composable
fun CheckCircle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colors.secondary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            )
        } else {
            Modifier
        }
    Box(
        modifier = Modifier
            .size(32.dp)
            .then(modifier)
            .then(toggleableModifier),
    ) {
        Card(
            modifier = modifier.fillMaxSize(),
            shape = CircleShape,
            border = BorderStroke(2.dp, color)
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Done,
                    contentDescription = "Done",
                    tint = color,
                )
            }
        }
    }
}

expect fun getPlatformName(): String