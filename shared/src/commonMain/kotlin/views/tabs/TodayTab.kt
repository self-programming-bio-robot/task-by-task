package views.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.icerock.moko.mvvm.compose.getViewModel
import dev.icerock.moko.mvvm.compose.viewModelFactory
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.getKoin
import services.TodoService
import views.NewTaskInput
import views.TodoList
import viewsModels.TimerViewModel
import viewsModels.TodoListViewModel

internal object TodayTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Filled.Today)
            return remember {
                TabOptions(
                    index = 0u,
                    title = "Today",
                    icon = icon
                )
            }
        }

    @Composable
    override fun Content() {
        var todoService = getKoin().get<TodoService>()
        var timerViewModel = getKoin().get<TimerViewModel>()
        var todoListViewModel =
            getViewModel(Unit, viewModelFactory { TodoListViewModel(todoService) })

        timerViewModel.onDone {
            todoListViewModel.updateItems()
        }

        Column {
            DayHeader(
                date = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).date
            )
            TodoList(
                todoListViewModel = todoListViewModel,
                modifier = Modifier.weight(1f)
            )
            NewTaskInput {
                todoListViewModel.addTodo(it)
            }
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