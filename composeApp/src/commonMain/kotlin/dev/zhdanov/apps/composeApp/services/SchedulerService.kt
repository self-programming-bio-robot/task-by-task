package dev.zhdanov.apps.composeApp.services

import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface SchedulerService {

    fun addScheduler(tag: String, cron: String, timeZone: TimeZone, action: SchedulerAction)

    fun addScheduler(tag: String, cron: String, action: SchedulerAction)

}

@OptIn(ExperimentalTime::class)
typealias SchedulerAction = (plannedTime: Instant, actualTime: Instant, timeZone: TimeZone) -> Unit
