package theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color

val Colors.timerWorkingState: Color
    get() = if (isLight) Color.Cyan else Color.Cyan

val Colors.timerRestState: Color
    get() = if (isLight) Color.Green else Color.Green