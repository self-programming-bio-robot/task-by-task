package views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

@Composable
fun NewTaskInput(onEnter: (text: String) -> Unit) {
    var newTodoTitle by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun send() {
        if (newTodoTitle.isNotBlank()) {
            onEnter(newTodoTitle)
            newTodoTitle = ""
            focusManager.clearFocus()
            isError = false
        } else {
            isError = true
        }
    }

    Row {
        TextField(newTodoTitle,
            modifier = Modifier.weight(1.0f),
            singleLine = true,
            isError = isError,
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