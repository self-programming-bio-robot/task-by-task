package dev.zhdanov.apps.shared.cache

import dev.zhdanov.apps.shared.cache.focus.CreateFocusTime

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
        return dbQuery.selectAllFocusTimes().executeAsList()
    }
}