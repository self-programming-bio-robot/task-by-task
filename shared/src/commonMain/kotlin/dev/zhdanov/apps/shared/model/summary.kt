package dev.zhdanov.apps.shared.model

import kotlinx.datetime.LocalDate

data class DaySummary(
    val date: LocalDate,
    val focusTime: Long,
    val review: String
)
