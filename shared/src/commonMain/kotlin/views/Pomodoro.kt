package views

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Pomodoro(size: PomodoroSize = PomodoroSize.MEDIUM) {
    Icon(
        modifier = Modifier.size(size.dp),
        imageVector = Icons.Rounded.AddCircle,
        contentDescription = null
    )
}

enum class PomodoroSize(val dp: Dp) {
    MEDIUM(24.dp),
    LARGE(32.dp)
}