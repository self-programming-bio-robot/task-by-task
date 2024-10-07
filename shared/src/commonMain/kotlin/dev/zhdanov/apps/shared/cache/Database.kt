package dev.zhdanov.apps.shared.cache

import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val driver = databaseDriverFactory.createDriver()
    private val database = AppDatabase(driver)
    private val dbQuery = database.appDatabaseQueries

    init {
        AppDatabase.Schema.create(driver)
    }

    fun addFocusTime(focusTime: CreateFocusTime) {
        addFocusTime(
            duration = focusTime.duration.toLong(),
            finishedAt = focusTime.finishedAt,
            feedback = focusTime.feedback
        )
    }

    fun addFocusTime(duration: Long, finishedAt: Long, feedback: String?) {
        dbQuery.transaction {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt
            )
        }
    }

    fun getAllFocusTimes(): List<FocusTime> {
        return dbQuery
            .selectAllFocusTimes(focusTimeMapper)
            .executeAsList()
    }

    fun getAllFocusTimesBetween(from: Long, to: Long): List<FocusTime> {
        return dbQuery
            .selectFocusTimesInPeriod(from, to, focusTimeMapper)
            .executeAsList()
    }

    fun addDaySummary(daySummary: DaySummary) {
        dbQuery.transaction {
            dbQuery.insertDaySummary(
                date = daySummary.date.toLong(),
                focusTime = daySummary.focusTime,
                review = daySummary.review
            )
        }
    }

    fun getAllDaySummaries(): List<DaySummary> {
        return dbQuery
            .selectAllDaySummaries(daySummaryMapper)
            .executeAsList()
    }

    fun getDaySummary(date: LocalDate): DaySummary? {
        return dbQuery
            .selectDaySummaryOnDate(date.toLong(), daySummaryMapper)
            .executeAsOneOrNull();
    }
}
