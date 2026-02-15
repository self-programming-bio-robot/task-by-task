package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusTimeWithTask(
    val focusTime: FocusTime,
    val task: Task?
)

/**
 * FocusTime with multiple linked tasks (many-to-many relationship)
 */
@Serializable
data class FocusTimeWithTasks(
    val focusTime: FocusTime,
    val tasks: List<Task>
)
