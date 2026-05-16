package dev.zhdanov.apps.shared.cache

import app.cash.sqldelight.db.QueryResult
import dev.zhdanov.apps.shared.cache.repository.SettingsRepository
import dev.zhdanov.apps.shared.cache.repository.TaskRepository
import dev.zhdanov.apps.shared.cache.repository.TimerSettingRepository
import dev.zhdanov.apps.shared.cache.repository.WorkspaceRepository
import dev.zhdanov.apps.shared.model.CreateFocusTime
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.DaySummaryRecord
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_BASE_URL
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_MODEL
import dev.zhdanov.apps.shared.model.DEFAULT_ENCRYPTION_ITERATIONS
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ICON
import dev.zhdanov.apps.shared.model.FocusTime
import dev.zhdanov.apps.shared.model.SettingKey
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLocalDate
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import com.diamondedge.logging.logging
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val driver = databaseDriverFactory.createDriver()
    private val database = AppDatabase(
        driver,
    )
    private val dbQuery = database.appDatabaseQueries

    val timerSettingRepository = TimerSettingRepository(dbQuery)
    val taskRepository = TaskRepository(dbQuery)
    val settingRepository = SettingsRepository(dbQuery)
    val workspaceRepository = WorkspaceRepository(dbQuery)

    init {
        val currentVersion = getDatabaseVersion()
        logger.i { "Current version: $currentVersion; actual version: ${AppDatabase.Schema.version}" }
        try {
            when {
                currentVersion == 0L -> AppDatabase.Schema.create(driver)
                currentVersion < AppDatabase.Schema.version -> {
                    ensurePreMigrationCompatibility()
                    AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
                }
                currentVersion > AppDatabase.Schema.version -> {
                    logger.w {
                        "Database version $currentVersion is newer than supported version ${AppDatabase.Schema.version}"
                    }
                }
            }
            ensureLegacyCompatibility()
            ensureWorkspaceCompatibility()
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

    private fun ensurePreMigrationCompatibility() {
        ensureTimerSettingTable()
        ensureSettingsTable()
    }

    private fun ensureTimerSettingTable() {
        if (tableExists("TimerSetting")) {
            return
        }

        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS TimerSetting (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                workDuration INTEGER NOT NULL,
                shortBreakDuration INTEGER NOT NULL,
                longBreakDuration INTEGER NOT NULL,
                workCycles INTEGER NOT NULL,
                isDefault INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            0
        )
    }

    private fun ensureSettingsTable() {
        if (tableExists("Settings")) {
            return
        }

        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS Settings (
                settingKey INTEGER NOT NULL PRIMARY KEY,
                data TEXT NOT NULL
            )
            """.trimIndent(),
            0
        )
    }

    private fun ensureWorkspaceCompatibility() {
        ensureWorkspaceTables()
        ensureColumn("Workspace", "icon", "TEXT NOT NULL DEFAULT '$DEFAULT_WORKSPACE_ICON'")
        ensureColumn("FocusTime", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
        ensureColumn("FocusTime", "syncId", "TEXT NOT NULL DEFAULT ''")
        ensureColumn("FocusTime", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
        ensureColumn("FocusTime", "deletedAt", "INTEGER")
        ensureColumn("DaySummary", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
        ensureColumn("DaySummary", "syncId", "TEXT NOT NULL DEFAULT ''")
        ensureColumn("DaySummary", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
        ensureColumn("DaySummary", "deletedAt", "INTEGER")
        ensureColumn("TimerSetting", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
        ensureColumn("TimerSetting", "syncId", "TEXT NOT NULL DEFAULT ''")
        ensureColumn("TimerSetting", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
        ensureColumn("TimerSetting", "deletedAt", "INTEGER")
        ensureColumn("Task", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
        ensureColumn("Task", "syncId", "TEXT NOT NULL DEFAULT ''")
        ensureColumn("Task", "updatedAt", "INTEGER NOT NULL DEFAULT 0")
        ensureColumn("Task", "deletedAt", "INTEGER")
        backfillWorkspaceMetadata()
        migrateLegacyOpenAiToken()
    }

    private fun ensureWorkspaceTables() {
        val now = Clock.System.now().toEpochMilliseconds()
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS Workspace (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                syncId TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                icon TEXT NOT NULL DEFAULT '$DEFAULT_WORKSPACE_ICON',
                isSelected INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
            """.trimIndent(),
            0
        )
        ensureColumn("Workspace", "icon", "TEXT NOT NULL DEFAULT '$DEFAULT_WORKSPACE_ICON'")
        driver.execute(
            null,
            """
            INSERT OR IGNORE INTO Workspace(id, syncId, name, icon, isSelected, createdAt, updatedAt)
            VALUES (1, 'local-workspace', 'Local workspace', '$DEFAULT_WORKSPACE_ICON', 1, $now, $now)
            """.trimIndent(),
            0
        )
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS WorkspaceSecuritySettings (
                workspaceId INTEGER NOT NULL PRIMARY KEY,
                openAiToken TEXT NOT NULL DEFAULT '',
                llmBaseUrl TEXT NOT NULL DEFAULT 'https://api.openai.com/v1/',
                llmModelId TEXT NOT NULL DEFAULT 'gpt-4.1',
                encryptionEnabled INTEGER NOT NULL DEFAULT 0,
                encryptionSalt TEXT,
                wrappedDataKey TEXT,
                encryptionIterations INTEGER NOT NULL DEFAULT 600000,
                FOREIGN KEY(workspaceId) REFERENCES Workspace(id)
            )
            """.trimIndent(),
            0
        )
        workspaceRepository.ensureSecuritySettings(DEFAULT_WORKSPACE_ID)
        workspaceRepository.ensureDefaultWorkspace()
    }

    private fun backfillWorkspaceMetadata() {
        if (tableExists("FocusTime")) {
            driver.execute(null, "UPDATE FocusTime SET syncId = 'legacy-focus-' || id WHERE syncId = ''", 0)
            driver.execute(null, "UPDATE FocusTime SET updatedAt = finishedAt WHERE updatedAt = 0", 0)
        }
        if (tableExists("DaySummary")) {
            driver.execute(null, "UPDATE DaySummary SET syncId = 'legacy-summary-' || date WHERE syncId = ''", 0)
            driver.execute(null, "UPDATE DaySummary SET updatedAt = date WHERE updatedAt = 0", 0)
        }
        if (tableExists("TimerSetting")) {
            driver.execute(null, "UPDATE TimerSetting SET syncId = 'legacy-timer-' || id WHERE syncId = ''", 0)
            driver.execute(null, "UPDATE TimerSetting SET updatedAt = ${Clock.System.now().toEpochMilliseconds()} WHERE updatedAt = 0", 0)
        }
        if (tableExists("Task")) {
            driver.execute(null, "UPDATE Task SET syncId = 'legacy-task-' || id WHERE syncId = ''", 0)
            driver.execute(null, "UPDATE Task SET updatedAt = createdAt WHERE updatedAt = 0", 0)
        }
    }

    private fun migrateLegacyOpenAiToken() {
        val existing = workspaceRepository.getSecuritySettings(DEFAULT_WORKSPACE_ID)
        if (existing == null || existing.openAiToken.isNotBlank()) {
            return
        }

        val token = runCatching {
            settingRepository.getSetting<String>(SettingKey.OPENAI_TOKEN)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return

        workspaceRepository.updateAssistantConfig(
            workspaceId = DEFAULT_WORKSPACE_ID,
            token = token,
            baseUrl = existing.llmBaseUrl.ifBlank { DEFAULT_ASSISTANT_BASE_URL },
            modelId = existing.llmModelId.ifBlank { DEFAULT_ASSISTANT_MODEL }
        )
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

    fun addFocusTime(focusTime: CreateFocusTime, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        addFocusTime(
            duration = focusTime.duration.toLong(),
            finishedAt = focusTime.finishedAt,
            feedback = focusTime.feedback,
            startedAt = focusTime.startedAt,
            pauseTime = focusTime.pauseTime?.toLong(),
            taskId = focusTime.taskId,
            workspaceId = workspaceId
        )
    }

    fun transaction(block: () -> Unit) {
        dbQuery.transaction {
            block()
        }
    }

    fun addFocusTime(
        duration: Long,
        finishedAt: Long,
        feedback: String?,
        startedAt: Long? = null,
        pauseTime: Long? = null,
        taskId: Long? = null,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        dbQuery.transaction {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime,
                taskId = taskId,
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = now
            )
        }
    }

    /**
     * Add a FocusTime and return its generated ID.
     */
    fun addFocusTimeAndGetId(
        duration: Long,
        finishedAt: Long,
        feedback: String?,
        startedAt: Long? = null,
        pauseTime: Long? = null,
        taskId: Long? = null,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return dbQuery.transactionWithResult {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime,
                taskId = taskId,
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = now
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
        taskIds: List<Long> = emptyList(),
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return dbQuery.transactionWithResult {
            dbQuery.insertFocusTime(
                duration = duration,
                feedback = feedback,
                finishedAt = finishedAt,
                startedAt = startedAt,
                pauseTime = pauseTime,
                taskId = taskIds.firstOrNull(), // Keep backward compatibility with single taskId column
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = now
            )
            val focusTimeId = dbQuery.lastInsertRowId().executeAsOne()

            // Link all tasks via junction table
            taskIds.forEach { taskId ->
                dbQuery.insertFocusTimeTaskCrossRef(focusTimeId, taskId)
            }

            focusTimeId
        }
    }

    fun getAllFocusTimes(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<FocusTime> {
        return dbQuery
            .selectAllFocusTimes(workspaceId, focusTimeMapper)
            .executeAsList()
    }

    fun updateFocusTimeFeedback(id: Long, feedback: String, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        dbQuery.updateFocusTimeFeedback(
            feedback = feedback,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId,
            id = id
        )
    }

    fun getAllFocusTimesBetween(from: Long, to: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID): List<FocusTime> {
        return dbQuery
            .selectFocusTimesInPeriod(workspaceId, from, to, focusTimeMapper)
            .executeAsList()
    }

    // Many-to-many: FocusTime <-> Task
    fun linkTaskToFocusTime(focusTimeId: Long, taskId: Long) {
        dbQuery.insertFocusTimeTaskCrossRef(focusTimeId, taskId)
    }

    fun unlinkTaskFromFocusTime(focusTimeId: Long, taskId: Long) {
        dbQuery.deleteFocusTimeTaskCrossRef(focusTimeId, taskId)
    }

    fun getTasksForFocusTime(focusTimeId: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID): List<Task> {
        return dbQuery
            .selectTasksForFocusTime(workspaceId, focusTimeId, taskMapper)
            .executeAsList()
    }

    fun getFocusTimesForTask(taskId: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID): List<FocusTime> {
        return dbQuery
            .selectFocusTimesForTask(workspaceId, taskId, focusTimeMapper)
            .executeAsList()
    }

    fun addDaySummary(daySummary: DaySummary, workspaceId: Long = daySummary.workspaceId) {
        addDaySummaryRaw(
            date = daySummary.date,
            focusTime = daySummary.focusTime,
            review = daySummary.review,
            linkedTasks = json.encodeToString(daySummary.linkedTasks),
            workspaceId = workspaceId
        )
    }

    fun addDaySummaryRaw(
        date: LocalDate,
        focusTime: Long,
        review: String,
        linkedTasks: String,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        dbQuery.transaction {
            dbQuery.insertDaySummary(
                date = date.toLong(),
                focusTime = focusTime,
                review = review,
                linkedTasks = linkedTasks,
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    fun getAllDaySummaries(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<DaySummary> {
        return dbQuery
            .selectAllDaySummaries(workspaceId, daySummaryMapper)
            .executeAsList()
    }

    fun getDaySummary(date: LocalDate, workspaceId: Long = DEFAULT_WORKSPACE_ID): DaySummary? {
        return dbQuery
            .selectDaySummaryOnDate(workspaceId, date.toLong(), daySummaryMapper)
            .executeAsOneOrNull()
    }

    fun getAllDaySummaryRecords(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<DaySummaryRecord> {
        return dbQuery
            .selectAllDaySummaries(workspaceId, daySummaryRecordMapper)
            .executeAsList()
    }

    fun getDaySummaryRecord(date: LocalDate, workspaceId: Long = DEFAULT_WORKSPACE_ID): DaySummaryRecord? {
        return dbQuery
            .selectDaySummaryOnDate(workspaceId, date.toLong(), daySummaryRecordMapper)
            .executeAsOneOrNull()
    }

    fun updateDaySummaryEncryptedFields(
        date: LocalDate,
        review: String,
        linkedTasks: String,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        dbQuery.updateDaySummaryEncryptedFields(
            review = review,
            linkedTasks = linkedTasks,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId,
            date = date.toLong()
        )
    }

    fun updateTaskEncryptedFields(
        id: Long,
        title: String,
        description: String?,
        workspaceId: Long = DEFAULT_WORKSPACE_ID
    ) {
        dbQuery.updateTaskEncryptedFields(
            title = title,
            description = description,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            workspaceId = workspaceId,
            id = id
        )
    }

    companion object {
        val logger = logging(Database::class.qualifiedName)
        private val json = Json { ignoreUnknownKeys = true }
    }
}
