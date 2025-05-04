package dev.zhdanov.apps.composeApp.services

import com.ucasoft.kcron.Cron
import com.ucasoft.kcron.core.builders.Builder
import com.ucasoft.kcron.core.builders.DelicateIterableApi
import com.ucasoft.kcron.core.common.WeekDays
import com.ucasoft.kcron.kotlinx.datetime.CronLocalDateTime
import com.ucasoft.kcron.kotlinx.datetime.CronLocalDateTimeProvider
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.lighthousegames.logging.logging
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class DesktopScheduler(
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) : SchedulerService {

    private val schedulers = PriorityQueue<Scheduler> { s1, s2 -> s1.nextRun.compareTo(s2.nextRun) }

    init {
        loop()
    }

    @OptIn(DelicateIterableApi::class)
    override fun addScheduler(tag: String, cron: String, timeZone: TimeZone, action: SchedulerAction) {
        val cronBuilder = Cron.parseAndBuild(cron) {
            it.firstDayOfWeek = WeekDays.Monday
        }

        addRun(cronBuilder, tag, timeZone, action)
    }

    override fun addScheduler(tag: String, cron: String, action: SchedulerAction) {
        this.addScheduler(tag, cron, TimeZone.currentSystemDefault(), action)
    }

    private fun loop() {
        executor.scheduleAtFixedRate(
            {
                if (schedulers.isNotEmpty()) {
                    val scheduler = schedulers.peek()
                    val duration = scheduler.nextRun.minus(Clock.System.now())

                    if (duration.isNegative()) {
                        schedulers.poll()
                        logger.i { "Run scheduler: '${scheduler.tag}'" }

                        addRun(scheduler.cron, scheduler.tag, scheduler.timeZone, scheduler.action)
                        scheduler.action.invoke(scheduler.nextRun, Clock.System.now(), scheduler.timeZone)
                    }
                }
            },
            0L, 1L, TimeUnit.SECONDS
        )
    }

    private fun addRun(
        cronBuilder: Builder<LocalDateTime, CronLocalDateTime, CronLocalDateTimeProvider>,
        tag: String,
        timeZone: TimeZone,
        action: SchedulerAction
    ) {
        cronBuilder.nextRun?.let { next ->
            schedulers.add(Scheduler(tag, cronBuilder, next.toInstant(timeZone), timeZone, action))
            val scheduler = schedulers.peek()
            logger.d { "Next run: '${scheduler.tag}' at ${scheduler.nextRun}" }
        }
    }

    companion object {
        private val logger = logging(DesktopScheduler::class.qualifiedName)
    }

    data class Scheduler(
        val tag: String,
        val cron: Builder<LocalDateTime, CronLocalDateTime, CronLocalDateTimeProvider>,
        val nextRun: Instant,
        val timeZone: TimeZone,
        val action: SchedulerAction
    )
}
