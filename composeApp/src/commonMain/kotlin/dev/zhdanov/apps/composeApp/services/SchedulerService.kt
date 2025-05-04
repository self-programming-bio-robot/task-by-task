package dev.zhdanov.apps.composeApp.services

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

interface SchedulerService {

    fun addScheduler(tag: String, cron: String, timeZone: TimeZone, action: SchedulerAction)

    fun addScheduler(tag: String, cron: String, action: SchedulerAction)

}

typealias SchedulerAction = (plannedTime: Instant, actualTime: Instant, timeZone: TimeZone) -> Unit
