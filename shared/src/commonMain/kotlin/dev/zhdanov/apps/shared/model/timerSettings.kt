package dev.zhdanov.apps.shared.model

data class TimerSettings(
    val id: Long = -1,
    val default: Boolean,
    val workDuration: Int,   // in seconds
    val shortBreakDuration: Int,  // in seconds
    val longBreakDuration: Int,   // in seconds
    val workCycles: Int,   // number of work sessions before a long break
    val isInfinite: Boolean = false,
    val workspaceId: Long = DEFAULT_WORKSPACE_ID,
    val syncId: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)
