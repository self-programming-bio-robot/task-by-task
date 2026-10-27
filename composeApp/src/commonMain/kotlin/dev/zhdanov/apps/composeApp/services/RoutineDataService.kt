package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.CreateRoutine
import dev.zhdanov.apps.shared.model.CreateTask
import dev.zhdanov.apps.shared.model.Routine
import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

enum class RoutinePeriod(val days: Int, val title: String) {
    WEEK(7, "Week"),
    TWO_WEEKS(14, "2 Weeks"),
    MONTH(30, "Month")
}

enum class CellStatus {
    COMPLETED,
    MISSED,
    PENDING,
    INACTIVE
}

data class RoutineMatrixCell(
    val date: LocalDate,
    val status: CellStatus,
    val taskId: Long? = null
)

data class RoutineReportRow(
    val routine: Routine,
    val cells: List<RoutineMatrixCell>,
    val completionRate: Float,
    val currentStreak: Int
)

class RoutineDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers,
    private val workspaceSessionService: WorkspaceSessionService,
    private val logicalDayService: LogicalDayService
) {
    val routineEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    suspend fun getAllRoutines(): List<Routine> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        database.routineRepository.getAllRoutines(workspaceId)
            .map(::decryptRoutine)
    }

    suspend fun getActiveRoutines(): List<Routine> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        database.routineRepository.getActiveRoutines(workspaceId)
            .map(::decryptRoutine)
    }

    suspend fun addRoutine(
        title: String,
        description: String? = null,
        scheduleType: String = "DAILY",
        currentDate: LocalDate? = null
    ): Long = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        val encryptedTitle = workspaceSessionService.encryptTextForCurrentWorkspace(title.trim())
        val encryptedDesc = workspaceSessionService.encryptNullableTextForCurrentWorkspace(description?.trim())

        val routineId = database.routineRepository.addRoutine(
            CreateRoutine(
                title = encryptedTitle,
                description = encryptedDesc,
                scheduleType = scheduleType,
                isActive = true
            ),
            workspaceId = workspaceId
        )

        val today = currentDate ?: logicalDayService.currentLogicalDay()
        ensureRoutineTaskForDate(
            routineId = routineId,
            encryptedTitle = encryptedTitle,
            encryptedDescription = encryptedDesc,
            date = today,
            workspaceId = workspaceId
        )
        routineEvents.tryEmit(Unit)
        routineId
    }

    suspend fun convertTaskToRoutine(
        task: Task,
        currentDate: LocalDate? = null
    ): Long = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        val encryptedTitle = workspaceSessionService.encryptTextForCurrentWorkspace(task.title.trim())
        val encryptedDesc = workspaceSessionService.encryptNullableTextForCurrentWorkspace(task.description?.trim())

        val routineId = database.routineRepository.addRoutine(
            CreateRoutine(
                title = encryptedTitle,
                description = encryptedDesc,
                scheduleType = "DAILY",
                isActive = true
            ),
            workspaceId = workspaceId
        )

        val today = currentDate ?: logicalDayService.currentLogicalDay()
        database.taskRepository.attachTaskToRoutine(
            taskId = task.id,
            routineId = routineId,
            targetDate = today.toLong(),
            workspaceId = workspaceId
        )
        routineEvents.tryEmit(Unit)
        routineId
    }

    suspend fun updateRoutine(
        routine: Routine,
        currentDate: LocalDate? = null
    ) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        val encryptedTitle = workspaceSessionService.encryptTextForCurrentWorkspace(routine.title.trim())
        val encryptedDesc = workspaceSessionService.encryptNullableTextForCurrentWorkspace(
            routine.description?.trim()?.takeIf { it.isNotEmpty() }
        )
        val encryptedRoutine = routine.copy(
            title = encryptedTitle,
            description = encryptedDesc
        )
        database.routineRepository.updateRoutine(encryptedRoutine, workspaceId)

        val today = currentDate ?: logicalDayService.currentLogicalDay()
        val existingTodayTask = database.taskRepository.getTaskByRoutineAndDate(
            routineId = routine.id,
            targetDate = today.toLong(),
            workspaceId = workspaceId
        )
        if (existingTodayTask != null && !existingTodayTask.isCompleted) {
            database.taskRepository.updateTask(
                existingTodayTask.copy(
                    title = encryptedTitle,
                    description = encryptedDesc
                ),
                workspaceId = workspaceId
            )
        } else if (routine.isActive && existingTodayTask == null) {
            ensureRoutineTaskForDate(
                routineId = routine.id,
                encryptedTitle = encryptedTitle,
                encryptedDescription = encryptedDesc,
                date = today,
                workspaceId = workspaceId
            )
        }
        routineEvents.tryEmit(Unit)
    }

    suspend fun toggleRoutineActive(
        id: Long,
        currentDate: LocalDate? = null
    ) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        val routine = database.routineRepository.getRoutineById(id, workspaceId) ?: return@withContext
        val newActive = !routine.isActive
        database.routineRepository.updateRoutine(routine.copy(isActive = newActive), workspaceId)
        if (newActive) {
            val today = currentDate ?: logicalDayService.currentLogicalDay()
            ensureRoutineTaskForDate(
                routineId = routine.id,
                encryptedTitle = routine.title,
                encryptedDescription = routine.description,
                date = today,
                workspaceId = workspaceId
            )
        }
        routineEvents.tryEmit(Unit)
    }

    suspend fun deleteRoutine(id: Long) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        database.routineRepository.deleteRoutine(id, workspaceId)
        routineEvents.tryEmit(Unit)
    }

    suspend fun syncDailyRoutineTasks(currentDate: LocalDate? = null) = withContext(dispatchers.io) {
        if (workspaceSessionService.isCurrentWorkspaceLocked.value) return@withContext
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        val activeRoutines = database.routineRepository.getActiveRoutines(workspaceId)
        if (activeRoutines.isEmpty()) return@withContext

        val today = currentDate ?: logicalDayService.currentLogicalDay()
        var createdAny = false
        for (routine in activeRoutines) {
            val createdDate = logicalDayService.logicalDayFor(routine.createdAt)
            if (createdDate <= today) {
                val created = ensureRoutineTaskForDate(
                    routineId = routine.id,
                    encryptedTitle = routine.title,
                    encryptedDescription = routine.description,
                    date = today,
                    workspaceId = workspaceId
                )
                if (created) createdAny = true
            }
        }
        if (createdAny) {
            routineEvents.tryEmit(Unit)
        }
    }

    private fun ensureRoutineTaskForDate(
        routineId: Long,
        encryptedTitle: String,
        encryptedDescription: String?,
        date: LocalDate,
        workspaceId: Long
    ): Boolean {
        val dateLong = date.toLong()
        val existingTask = database.taskRepository.getTaskByRoutineAndDate(routineId, dateLong, workspaceId)
        if (existingTask != null) return false
        database.taskRepository.addTask(
            CreateTask(
                title = encryptedTitle,
                description = encryptedDescription,
                routineId = routineId,
                targetDate = dateLong
            ),
            workspaceId = workspaceId
        )
        return true
    }

    suspend fun getRoutineMatrixReport(
        period: RoutinePeriod,
        currentDate: LocalDate? = null,
        periodOffset: Int = 0
    ): List<RoutineReportRow> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        val workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        val routines = database.routineRepository.getAllRoutines(workspaceId).map(::decryptRoutine)
        if (routines.isEmpty()) return@withContext emptyList()

        val today = currentDate ?: logicalDayService.currentLogicalDay()
        val endDate = today.plus(periodOffset * period.days, DateTimeUnit.DAY)
        val startDate = endDate.minus(period.days - 1, DateTimeUnit.DAY)
        val startLong = startDate.toLong()
        val endLong = endDate.toLong()

        val routineTasks = database.taskRepository.getRoutineTasksInPeriod(startLong, endLong, workspaceId)
            .groupBy { it.routineId to it.targetDate }

        val completedDatesByRoutine = database.taskRepository
            .getCompletedRoutineTasksUpToDate(today.toLong(), workspaceId)
            .groupBy({ it.routineId }, { it.targetDate })
            .mapValues { (_, dates) -> dates.filterNotNull().toSet() }

        routines.map { routine ->
            val createdDate = logicalDayService.logicalDayFor(routine.createdAt)
            val cells = mutableListOf<RoutineMatrixCell>()
            var activeDaysCount = 0
            var completedDaysCount = 0

            var date = startDate
            while (date <= endDate) {
                val dateLong = date.toLong()
                val task = routineTasks[routine.id to dateLong]?.firstOrNull()

                val status = when {
                    date < createdDate || date > today -> CellStatus.INACTIVE
                    task != null && task.isCompleted -> CellStatus.COMPLETED
                    date < today -> if (!routine.isActive && task == null) CellStatus.INACTIVE else CellStatus.MISSED
                    !routine.isActive && task == null -> CellStatus.INACTIVE
                    else -> CellStatus.PENDING
                }

                if (status != CellStatus.INACTIVE) {
                    activeDaysCount++
                    if (status == CellStatus.COMPLETED) {
                        completedDaysCount++
                    }
                }

                cells.add(RoutineMatrixCell(date = date, status = status, taskId = task?.id))
                date = date.plus(1, DateTimeUnit.DAY)
            }

            val completionRate = if (activeDaysCount > 0) completedDaysCount.toFloat() / activeDaysCount else 0f

            // Calculate full current streak backwards from today (not bounded by period window)
            val completedDates = completedDatesByRoutine[routine.id].orEmpty()
            var streak = 0
            var checkDate = today
            while (true) {
                val dateLong = checkDate.toLong()
                if (dateLong in completedDates) {
                    streak++
                    checkDate = checkDate.minus(1, DateTimeUnit.DAY)
                } else if (checkDate == today && checkDate >= createdDate) {
                    checkDate = checkDate.minus(1, DateTimeUnit.DAY)
                } else {
                    break
                }
            }

            RoutineReportRow(
                routine = routine,
                cells = cells,
                completionRate = completionRate,
                currentStreak = streak
            )
        }
    }

    private fun decryptRoutine(routine: Routine): Routine {
        return routine.copy(
            title = workspaceSessionService.decryptTextForCurrentWorkspace(routine.title),
            description = workspaceSessionService.decryptNullableTextForCurrentWorkspace(routine.description)
        )
    }
}
