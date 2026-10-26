package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.cache.routineMapper
import dev.zhdanov.apps.shared.model.CreateRoutine
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.Routine
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class RoutineRepository(private val database: AppDatabaseQueries) {

    fun addRoutine(routine: CreateRoutine, workspaceId: Long = DEFAULT_WORKSPACE_ID): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.transactionWithResult {
            database.insertRoutine(
                title = routine.title,
                description = routine.description,
                scheduleType = routine.scheduleType,
                isActive = routine.isActive,
                createdAt = now,
                workspaceId = workspaceId,
                syncId = Uuid.random().toString(),
                updatedAt = now
            )
            database.lastInsertRowId().executeAsOne()
        }
    }

    fun getAllRoutines(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<Routine> {
        return database.selectAllRoutines(workspaceId, routineMapper).executeAsList()
    }

    fun getActiveRoutines(workspaceId: Long = DEFAULT_WORKSPACE_ID): List<Routine> {
        return database.selectActiveRoutines(workspaceId, routineMapper).executeAsList()
    }

    fun getRoutineById(id: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID): Routine? {
        return database.selectRoutineById(workspaceId, id, routineMapper).executeAsOneOrNull()
    }

    fun updateRoutine(routine: Routine, workspaceId: Long = routine.workspaceId) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.updateRoutine(
            title = routine.title,
            description = routine.description,
            scheduleType = routine.scheduleType,
            isActive = routine.isActive,
            updatedAt = now,
            workspaceId = workspaceId,
            id = routine.id
        )
    }

    fun deleteRoutine(id: Long, workspaceId: Long = DEFAULT_WORKSPACE_ID) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.softDeleteRoutine(deletedAt = now, updatedAt = now, workspaceId = workspaceId, id = id)
    }
}
