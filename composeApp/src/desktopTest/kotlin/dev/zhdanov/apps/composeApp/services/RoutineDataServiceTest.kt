package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.model.CreateRoutine
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineDataServiceTest {
    @Test
    fun `addRoutine creates today task instance without backfilling missed past days`() = runTest {
        val (database, taskService, routineService) = createServices()
        val today = LocalDate(2026, 5, 16)

        val routineId = routineService.addRoutine(
            title = "Drink water",
            description = "2L daily",
            currentDate = today
        )

        val allTasks = taskService.getAllTasks()
        assertEquals(1, allTasks.size)
        val task = allTasks.single()
        assertEquals("Drink water", task.title)
        assertEquals("2L daily", task.description)
        assertEquals(routineId, task.routineId)
        assertEquals(today.toLong(), task.targetDate)
        assertTrue(task.isToday)
        assertNotNull(database.taskRepository.getTaskByRoutineAndDate(routineId, today.toLong()))
    }

    @Test
    fun `updateRoutine updates routine template and propagates description to today uncompleted task`() = runTest {
        val (_, taskService, routineService) = createServices()
        val today = LocalDate(2026, 5, 16)

        val routineId = routineService.addRoutine(
            title = "Morning Run",
            description = null,
            currentDate = today
        )
        val createdRoutine = routineService.getAllRoutines().single { it.id == routineId }

        routineService.updateRoutine(
            createdRoutine.copy(
                title = "Morning Run 5km",
                description = "Warm up 5 min, then 5km easy pace"
            ),
            currentDate = today
        )

        val updatedRoutine = routineService.getAllRoutines().single()
        assertEquals("Morning Run 5km", updatedRoutine.title)
        assertEquals("Warm up 5 min, then 5km easy pace", updatedRoutine.description)

        val todayTask = taskService.getAllTasks().single()
        assertEquals("Morning Run 5km", todayTask.title)
        assertEquals("Warm up 5 min, then 5km easy pace", todayTask.description)
    }

    @Test
    fun `convertTaskToRoutine links existing task without creating duplicate`() = runTest {
        val (_, taskService, routineService) = createServices()
        val today = LocalDate(2026, 5, 16)

        taskService.addTask("Journal", isToday = false)
        val originalTask = taskService.getAllTasks().single()

        val routineId = routineService.convertTaskToRoutine(originalTask, currentDate = today)
        routineService.syncDailyRoutineTasks(currentDate = today)

        val tasksAfterSync = taskService.getAllTasks()
        assertEquals(1, tasksAfterSync.size)
        val linked = tasksAfterSync.single()
        assertEquals(originalTask.id, linked.id)
        assertEquals(routineId, linked.routineId)
        assertEquals(today.toLong(), linked.targetDate)
        assertTrue(linked.isToday)
    }

    @Test
    fun `getRoutineMatrixReport calculates cells across periods and full streak beyond period window`() = runTest {
        val (database, _, routineService, logicalDayService) = createServices()
        val today = logicalDayService.currentLogicalDay()
        val routineId = database.routineRepository.addRoutine(
            CreateRoutine(title = "Stretch")
        )

        // Complete 10 consecutive days leading up to yesterday, leave today pending
        for (daysAgo in 1..10) {
            val date = today.minus(daysAgo, DateTimeUnit.DAY)
            database.taskRepository.addTask(
                CreateTask(
                    title = "Stretch",
                    isToday = false,
                    routineId = routineId,
                    targetDate = date.toLong()
                )
            )
            val added = database.taskRepository.getTaskByRoutineAndDate(routineId, date.toLong())!!
            database.taskRepository.completeTask(added.id)
        }
        routineService.syncDailyRoutineTasks(today)

        val weekReport = routineService.getRoutineMatrixReport(RoutinePeriod.WEEK, currentDate = today).single()
        val twoWeeksReport = routineService.getRoutineMatrixReport(RoutinePeriod.TWO_WEEKS, currentDate = today).single()
        val monthReport = routineService.getRoutineMatrixReport(RoutinePeriod.MONTH, currentDate = today).single()

        assertEquals(7, weekReport.cells.size)
        assertEquals(14, twoWeeksReport.cells.size)
        assertEquals(30, monthReport.cells.size)
        assertEquals(10, weekReport.currentStreak)
        assertEquals(10, monthReport.currentStreak)
        assertEquals(CellStatus.PENDING, weekReport.cells.last().status)
    }

    private data class ServicesFixture(
        val database: Database,
        val taskService: TaskDataService,
        val routineService: RoutineDataService,
        val logicalDayService: LogicalDayService
    )

    private fun createServices(): ServicesFixture {
        val database = Database(object : DatabaseDriverFactory {
            override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        })
        val workspaceSessionService = createWorkspaceSessionService(database)
        val dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher()
        )
        val settingsService = AppSettingsService(database, dispatchers, workspaceSessionService)
        val logicalDayService = LogicalDayService(settingsService)
        val taskDataService = TaskDataService(database, dispatchers, workspaceSessionService)
        val routineDataService = RoutineDataService(
            database = database,
            dispatchers = dispatchers,
            workspaceSessionService = workspaceSessionService,
            logicalDayService = logicalDayService
        )
        return ServicesFixture(database, taskDataService, routineDataService, logicalDayService)
    }
}
