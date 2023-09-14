package models

import kotlinx.datetime.Instant
import kotlin.time.Duration

data class TodoItem(
    val id: Long,
    val title: String,
    val description: String = "",
    val pomodoros: List<Pomodoro> = listOf(),
)

data class Pomodoro(
    val duration: Duration,
    val start: Instant,
    val isDone: Boolean,
)
