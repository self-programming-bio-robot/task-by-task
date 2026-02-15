package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class FocusTimeWithTask(
    val focusTime: FocusTime,
    val task: Task?
)
