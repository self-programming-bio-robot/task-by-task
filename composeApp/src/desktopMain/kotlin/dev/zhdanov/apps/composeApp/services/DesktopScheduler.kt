package dev.zhdanov.apps.composeApp.services

import com.ucasoft.kcron.Cron
import com.ucasoft.kcron.core.builders.DelicateIterableApi
import com.ucasoft.kcron.core.common.WeekDays
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.lighthousegames.logging.KmLog
import org.lighthousegames.logging.logging

class DesktopScheduler(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
): SchedulerService {

    @OptIn(DelicateIterableApi::class)
    override fun addScheduler(tag: String, cron: String, timeZone: TimeZone, action: SchedulerAction) {
        val cronBuilder = Cron.parseAndBuild(cron) {
            it.firstDayOfWeek = WeekDays.Monday
        }

        val iterator = cronBuilder.asIterable(Clock.System.now().toLocalDateTime(timeZone)).iterator()

        coroutineScope.launch {
            while (this.isActive && iterator.hasNext()) {
                val next = iterator.next()
                logger.d { "Next scheduling '$tag' at $next" }
                val duration = next.toInstant(timeZone).minus(Clock.System.now())
                delay(duration)
                logger.i { "Run scheduler: '$tag'" }
                action.invoke()
            }
        }
    }

    override fun addScheduler(tag: String, cron: String, action: SchedulerAction) {
        this.addScheduler(tag, cron, TimeZone.currentSystemDefault(), action)
    }

    companion object {
        private val logger = logging(DesktopScheduler::class.qualifiedName)
    }
}
