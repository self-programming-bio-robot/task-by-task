package dev.zhdanov.apps.shared.cache

import app.cash.sqldelight.db.QueryResult
import dev.zhdanov.apps.shared.cache.repository.SettingsRepository
import dev.zhdanov.apps.shared.cache.repository.TaskRepository
import dev.zhdanov.apps.shared.cache.repository.TimerSettingRepository
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLocalDate
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
            when {
                currentVersion == 0L -> AppDatabase.Schema.create(driver)
                currentVersion < AppDatabase.Schema.version -> {
                    AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
                }
                currentVersion > AppDatabase.Schema.version -> {
                    logger.w {
                        "Database version $currentVersion is newer than supported version ${AppDatabase.Schema.version}"
                    }
                }
            }
            ensureLegacyCompatibility()
        } catch (e: Exception) {
            logger.e(e) { "Failed to migrate app database" }
            throw IllegalStateException("Failed to initialize app database", e)
        }
    }

    private fun getDatabaseVersion(): Long {
        val executeQuery: QueryResult<Long> = driver.executeQuery(1, "PRAGMA user_version;", mapper = {
            val version = if (it.next().value) {
                it.getLong(0) ?: 0L
            } else {
                0L
            }
            QueryResult.Value(version)
        }, 0)
        return executeQuery.value
    }

    private fun ensureLegacyCompatibility() {
        ensureColumn("FocusTime", "startedAt", "INTEGER")
        ensureColumn("FocusTime", "pauseTime", "INTEGER")
        ensureColumn("FocusTime", "taskId", "INTEGER")
        ensureColumn("DaySummary", "linkedTasks", "TEXT NOT NULL DEFAULT '[]'")
        ensureFocusTimeTaskCrossRefTable()
    }

    private fun ensureColumn(tableName: String, columnName: String, definition: String) {
        requireSqlIdentifier(tableName)
        requireSqlIdentifier(columnName)

        if (!tableExists(tableName) || columnName in tableColumns(tableName)) {
            return
        }

        logger.i { "Adding missing legacy column $tableName.$columnName" }
        driver.execute(
            null,
            "ALTER TABLE $tableName ADD COLUMN $columnName $definition",
            0
        )
    }

    private fun ensureFocusTimeTaskCrossRefTable() {
        if (tableExists("FocusTimeTaskCrossRef") || !tableExists("FocusTime") || !tableExists("Task")) {
            return
        }

        logger.i { "Adding missing FocusTimeTaskCrossRef table" }
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS FocusTimeTaskCrossRef(
                focusTimeId INTEGER NOT NULL,
                taskId INTEGER NOT NULL,
                PRIMARY KEY(focusTimeId, taskId),
                FOREIGN KEY(focusTimeId) REFERENCES FocusTime(id),
                FOREIGN KEY(taskId) REFERENCES Task(id)
            )
            """.trimIndent(),
            0
        )
    }

    private fun tableExists(tableName: String): Boolean {
        requireSqlIdentifier(tableName)
        return driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$tableName'",
            mapper = { cursor ->
                QueryResult.Value(cursor.next().value)
            },
            0
        ).value
    }

    private fun tableColumns(tableName: String): Set<String> {
        requireSqlIdentifier(tableName)
        return driver.executeQuery(
            null,
            "PRAGMA table_info($tableName)",
            mapper = { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.next().value) {
                    cursor.getString(1)?.let(columns::add)
                }
                QueryResult.Value(columns)
            },
            0
        ).value
    }

    private fun requireSqlIdentifier(identifier: String) {
        require(identifier.all { it.isLetterOrDigit() || it == '_' }) {
            "Invalid SQL identifier: $identifier"
        }
    }

    fun addFocusTime(focusTime: CreateFocusTime) {
        addFocusTime(
            duration = focusTime.duration.toLong(),
            finishedAt = focusTime.finishedAt,
            feedback = focusTime.feedback,
            startedAt = focusTime.startedAt,
            pauseTime = focusTime.pauseTime?.toLong(),
            taskId = focusTime.taskId
        )
    }

    fun addFocusTime(duration: Long, finishedAt: Long, feedback: String?, startedAt: Long? = null, pauseTime: Long? = null, taskId: Long? = null) {
        dbQuery.transaction {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime,
                taskId = taskId
            )
        }
    }

    /**
     * Add a FocusTime and return its generated ID.
     */
    fun addFocusTimeAndGetId(duration: Long, finishedAt: Long, feedback: String?, startedAt: Long? = null, pauseTime: Long? = null, taskId: Long? = null): Long {
        return dbQuery.transactionWithResult {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime,
                taskId = taskId
            )
            dbQuery.lastInsertRowId().executeAsOne()
        }
    }

    /**
     * Add a FocusTime and link multiple tasks to it.
     */
    fun addFocusTimeWithTasks(
        duration: Long,
        finishedAt: Long,
        feedback: String?,
        startedAt: Long? = null,
        pauseTime: Long? = null,
        taskIds: List<Long> = emptyList()
    ): Long {
        return dbQuery.transactionWithResult {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime,
                taskId = taskIds.firstOrNull() // Keep backward compatibility with single taskId column
            )
            val focusTimeId = dbQuery.lastInsertRowId().executeAsOne()

            // Link all tasks via junction table
            taskIds.forEach { taskId ->
                dbQuery.insertFocusTimeTaskCrossRef(focusTimeId, taskId)
            }

            focusTimeId
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

    // Many-to-many: FocusTime <-> Task
    fun linkTaskToFocusTime(focusTimeId: Long, taskId: Long) {
        dbQuery.insertFocusTimeTaskCrossRef(focusTimeId, taskId)
    }

    fun unlinkTaskFromFocusTime(focusTimeId: Long, taskId: Long) {
        dbQuery.deleteFocusTimeTaskCrossRef(focusTimeId, taskId)
    }

    fun getTasksForFocusTime(focusTimeId: Long): List<Task> {
        return dbQuery
            .selectTasksForFocusTime(focusTimeId, taskMapper)
            .executeAsList()
    }

    fun getFocusTimesForTask(taskId: Long): List<FocusTime> {
        return dbQuery
            .selectFocusTimesForTask(taskId, focusTimeMapper)
            .executeAsList()
    }

    fun addDaySummary(daySummary: DaySummary) {
        dbQuery.transaction {
            dbQuery.insertDaySummary(
                date = daySummary.date.toLong(),
                focusTime = daySummary.focusTime,
                review = daySummary.review,
                linkedTasks = json.encodeToString(daySummary.linkedTasks)
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
            .executeAsOneOrNull()
    }

    companion object {
        val logger = logging(Database::class.qualifiedName)
        private val json = Json { ignoreUnknownKeys = true }
    }
}
