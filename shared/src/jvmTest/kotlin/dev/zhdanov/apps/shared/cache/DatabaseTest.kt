package dev.zhdanov.apps.shared.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.shared.StartOfDaySetting
import dev.zhdanov.apps.shared.model.CreateRoutine
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ICON
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.SettingKey
import dev.zhdanov.apps.shared.model.TaskSummary
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseTest {
    @Test
    fun `fresh database persists focus sessions with multiple linked tasks`() {
        val database = Database(InMemoryDriverFactory())
        database.taskRepository.addTask(CreateTask("Write tests"))
        database.taskRepository.addTask(CreateTask("Refactor database"))
        val taskIds = database.taskRepository.getAllTasks().map { it.id }

        val focusTimeId = database.addFocusTimeWithTasks(
            duration = 1_500,
            finishedAt = 1_000,
            feedback = "Good session",
            taskIds = taskIds
        )

        assertEquals(
            taskIds.sorted(),
            database.getTasksForFocusTime(focusTimeId).map { it.id }.sorted()
        )
    }

    @Test
    fun `day summaries preserve linked task snapshots`() {
        val database = Database(InMemoryDriverFactory())
        val date = LocalDate(2026, 5, 15)
        val linkedTasks = listOf(TaskSummary(1, "Audit", 1_500))

        database.addDaySummary(
            DaySummary(
                date = date,
                focusTime = 1_500,
                review = "Solid day",
                linkedTasks = linkedTasks
            )
        )

        assertEquals(linkedTasks, database.getDaySummary(date)?.linkedTasks)
    }

    @Test
    fun `settings repository round trips structured values`() {
        val database = Database(InMemoryDriverFactory())

        database.settingRepository.saveSetting(
            SettingKey.START_OF_DAY,
            StartOfDaySetting(hour = 6, minute = 30)
        )

        assertEquals(
            StartOfDaySetting(hour = 6, minute = 30),
            database.settingRepository.getSetting<StartOfDaySetting>(SettingKey.START_OF_DAY)
        )
    }

    @Test
    fun `workspace scoped tasks stay isolated`() {
        val database = Database(InMemoryDriverFactory())
        val secondWorkspace = database.workspaceRepository.createWorkspace("Client work")

        database.taskRepository.addTask(
            CreateTask("Default task"),
            workspaceId = DEFAULT_WORKSPACE_ID
        )
        database.taskRepository.addTask(CreateTask("Client task"), workspaceId = secondWorkspace.id)

        assertEquals(
            listOf("Default task"),
            database.taskRepository.getAllTasks(DEFAULT_WORKSPACE_ID).map { it.title }
        )
        assertEquals(
            listOf("Client task"),
            database.taskRepository.getAllTasks(secondWorkspace.id).map { it.title }
        )
    }

    @Test
    fun `workspace icon defaults and updates`() {
        val database = Database(InMemoryDriverFactory())
        val workspace = database.workspaceRepository.createWorkspace("Design")

        assertEquals(DEFAULT_WORKSPACE_ICON, workspace.icon)

        database.workspaceRepository.updateWorkspaceIcon(workspace.id, "folder")

        assertEquals("folder", database.workspaceRepository.getWorkspace(workspace.id)?.icon)
    }

    @Test
    fun `legacy OpenAI token migrates into default workspace security settings`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val queries = AppDatabase(driver).appDatabaseQueries
        queries.insertOrReplaceSetting(SettingKey.OPENAI_TOKEN.id, "\"legacy-token\"")

        val database = Database(SingleDriverFactory(driver))

        assertEquals(
            "legacy-token",
            database.workspaceRepository.getSecuritySettings(DEFAULT_WORKSPACE_ID)?.openAiToken
        )
    }

    @Test
    fun `legacy database gains missing focus and summary compatibility columns`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            "CREATE TABLE FocusTime (id INTEGER PRIMARY KEY AUTOINCREMENT, duration INTEGER NOT NULL, feedback TEXT, finishedAt INTEGER NOT NULL)",
            0
        )
        driver.execute(
            null,
            "CREATE TABLE DaySummary (date INTEGER NOT NULL, focusTime INTEGER NOT NULL, review TEXT NOT NULL)",
            0
        )
        driver.execute(
            null,
            "CREATE TABLE Task (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, description TEXT, createdAt INTEGER NOT NULL, completedAt INTEGER, isCompleted INTEGER NOT NULL DEFAULT 0, isToday INTEGER NOT NULL DEFAULT 0)",
            0
        )
        driver.execute(null, "PRAGMA user_version = 6", 0)

        val database = Database(SingleDriverFactory(driver))
        database.taskRepository.addTask(CreateTask("Legacy task"))
        val task = database.taskRepository.getAllTasks().first()
        val focusTimeId = database.addFocusTimeWithTasks(
            duration = 600,
            finishedAt = 1_000,
            feedback = "Legacy compatible",
            taskIds = listOf(task.id)
        )
        database.addDaySummary(
            DaySummary(
                date = LocalDate(2026, 5, 15),
                focusTime = 600,
                review = "Migrated",
                linkedTasks = listOf(TaskSummary(task.id, task.title, 600))
            )
        )

        assertEquals(listOf(task.id), database.getTasksForFocusTime(focusTimeId).map { it.id })
        assertNotNull(database.getDaySummary(LocalDate(2026, 5, 15)))
        assertTrue(database.getDaySummary(LocalDate(2026, 5, 15))!!.linkedTasks.isNotEmpty())
    }

    @Test
    fun `routine repository supports CRUD soft delete and task linking with Long targetDate`() {
        val database = Database(InMemoryDriverFactory())
        val routineId = database.routineRepository.addRoutine(
            CreateRoutine(title = "Morning Run", description = "5km")
        )

        val created = database.routineRepository.getRoutineById(routineId)
        assertNotNull(created)
        assertEquals("Morning Run", created.title)
        assertTrue(created.isActive)

        val dateLong = LocalDate(2026, 5, 16).toLong()
        assertEquals(20260516L, dateLong)

        database.taskRepository.addTask(
            CreateTask(
                title = "Morning Run",
                routineId = routineId,
                targetDate = dateLong
            )
        )

        val linkedTask = database.taskRepository.getTaskByRoutineAndDate(routineId, dateLong)
        assertNotNull(linkedTask)
        assertEquals(routineId, linkedTask.routineId)
        assertEquals(20260516L, linkedTask.targetDate)

        database.routineRepository.updateRoutine(created.copy(isActive = false))
        assertFalse(database.routineRepository.getRoutineById(routineId)!!.isActive)
        assertTrue(database.routineRepository.getActiveRoutines().isEmpty())

        database.routineRepository.deleteRoutine(routineId)
        assertNull(database.routineRepository.getRoutineById(routineId))
    }

    @Test
    fun `legacy duplicate day summaries on same date are deduplicated on init`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            "CREATE TABLE FocusTime (id INTEGER PRIMARY KEY AUTOINCREMENT, duration INTEGER NOT NULL, feedback TEXT, finishedAt INTEGER NOT NULL)",
            0
        )
        driver.execute(
            null,
            "CREATE TABLE DaySummary (date INTEGER NOT NULL, focusTime INTEGER NOT NULL, review TEXT NOT NULL)",
            0
        )
        driver.execute(
            null,
            "CREATE TABLE Task (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, description TEXT, createdAt INTEGER NOT NULL, completedAt INTEGER, isCompleted INTEGER NOT NULL DEFAULT 0, isToday INTEGER NOT NULL DEFAULT 0)",
            0
        )
        driver.execute(
            null,
            "INSERT INTO DaySummary (date, focusTime, review) VALUES (20261014, 600, 'First summary')",
            0
        )
        driver.execute(
            null,
            "INSERT INTO DaySummary (date, focusTime, review) VALUES (20261014, 1200, 'Updated summary')",
            0
        )
        driver.execute(null, "PRAGMA user_version = 6", 0)

        val database = Database(SingleDriverFactory(driver))
        val summaries = database.getAllDaySummaries()

        assertEquals(1, summaries.size)
        assertEquals(LocalDate(2026, 10, 14), summaries.single().date)
        assertEquals("Updated summary", summaries.single().review)
    }

    @Test
    fun `migration from v9 migrates isToday to targetDate and drops isToday column`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE Workspace (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                syncId TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                icon TEXT NOT NULL DEFAULT 'workspaces',
                isSelected INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
            """.trimIndent(),
            0
        )
        driver.execute(
            null,
            """
            CREATE TABLE Settings (
                settingKey INTEGER NOT NULL PRIMARY KEY,
                data TEXT NOT NULL
            )
            """.trimIndent(),
            0
        )
        driver.execute(
            null,
            "INSERT INTO Settings (settingKey, data) VALUES (5, '\"2026-05-16\"')",
            0
        )
        driver.execute(
            null,
            """
            CREATE TABLE Task (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                createdAt INTEGER NOT NULL,
                completedAt INTEGER,
                isCompleted INTEGER NOT NULL DEFAULT 0,
                isToday INTEGER NOT NULL DEFAULT 0,
                routineId INTEGER,
                targetDate INTEGER,
                workspaceId INTEGER NOT NULL DEFAULT 1,
                syncId TEXT NOT NULL DEFAULT '',
                updatedAt INTEGER NOT NULL DEFAULT 0,
                deletedAt INTEGER
            )
            """.trimIndent(),
            0
        )
        driver.execute(
            null,
            "INSERT INTO Task (title, createdAt, isToday, syncId, updatedAt) VALUES ('Today task', 1000, 1, 's1', 1000)",
            0
        )
        driver.execute(
            null,
            "INSERT INTO Task (title, createdAt, isToday, syncId, updatedAt) VALUES ('Backlog task', 1000, 0, 's2', 1000)",
            0
        )
        driver.execute(null, "PRAGMA user_version = 9", 0)

        val database = Database(SingleDriverFactory(driver))
        val tasks = database.taskRepository.getAllTasks().associateBy { it.title }

        assertEquals(20260516L, tasks.getValue("Today task").targetDate)
        assertNull(tasks.getValue("Backlog task").targetDate)
    }
}

private class InMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}

private class SingleDriverFactory(
    private val driver: SqlDriver
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = driver
}
