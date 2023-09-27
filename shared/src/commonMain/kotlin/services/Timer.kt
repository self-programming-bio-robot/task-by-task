package services

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import models.TimerBootstrap

interface Timer {
    fun updateTime(time: Instant): Boolean
    fun onDone(callback: () -> Unit)
}