package dev.zhdanov.apps.shared.cache

import app.cash.sqldelight.db.QueryResult
import dev.zhdanov.apps.shared.cache.repository.SettingsRepository
import dev.zhdanov.apps.shared.cache.repository.TaskRepository
import dev.zhdanov.apps.shared.cache.repository.TimerSettingRepository
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate
import com.diamondedge.logging.logging

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val driver = databaseDriverFactory.createDriver()
    private val database = AppDatabase(
        driver,
    )
    private val dbQuery = database.appDatabaseQueries

    val timerSettingRepository = TimerSettingRepository(dbQuery)
    val taskRepository = TaskRepository(dbQuery)
    val settingRepository = SettingsRepository(dbQuery)

    init {
        val currentVersion = getDatabaseVersion()
        logger.i { "Current version: $currentVersion; actual version: ${AppDatabase.Schema.version}" }
        try {
            AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
            AppDatabase.Schema.create(driver)
            // Manual migration for new columns
            migrateFocusTimeTable()
        } catch (e: Exception) {
            logger.e(e) { "Failed to migrate app database" }
        }
    }

    private fun getDatabaseVersion(): Long {
        val executeQuery: QueryResult<Long> = driver.executeQuery(1, "PRAGMA user_version;", mapper = {
            QueryResult.Value(it.getLong(0) ?: 0L)
        }, 0)
        return executeQuery.value
    }

    private fun migrateFocusTimeTable() {
        try {
            // Add startedAt column if it doesn't exist
            driver.execute(2, "ALTER TABLE FocusTime ADD COLUMN startedAt INTEGER", 0)
        } catch (e: Exception) {
            // Column already exists, ignore
        }
        try {
            // Add pauseTime column if it doesn't exist
            driver.execute(3, "ALTER TABLE FocusTime ADD COLUMN pauseTime INTEGER", 0)
        } catch (e: Exception) {
            // Column already exists, ignore
        }
    }

    fun addFocusTime(focusTime: CreateFocusTime) {
        addFocusTime(
            duration = focusTime.duration.toLong(),
            finishedAt = focusTime.finishedAt,
            feedback = focusTime.feedback,
            startedAt = focusTime.startedAt,
            pauseTime = focusTime.pauseTime?.toLong()
        )
    }

    fun addFocusTime(duration: Long, finishedAt: Long, feedback: String?, startedAt: Long? = null, pauseTime: Long? = null) {
        dbQuery.transaction {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime
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

    companion object {
        val logger = logging(Database::class.qualifiedName)
    }
}
