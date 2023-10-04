package views

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import models.TodoItem
import models.TodoSelectedEvent
import viewsModels.TodoListViewModel


@Composable
fun TodoList(
    modifier: Modifier,
    todoListViewModel: TodoListViewModel,
    onSelected: TodoSelectedEvent? = null
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
            uiState.list.forEach { item ->
                TodoItem(item) { newState ->
                    onSelected?.let { callback ->
                        callback(item)
                    } ?: todoListViewModel.switchTodo(item.id, newState)
                }
            }
            Row(Modifier.height(48.dp)) {}
        }
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
                        Pomodoro()
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
        Surface(
            modifier = modifier.fillMaxSize(),
            color = Color.Transparent,
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