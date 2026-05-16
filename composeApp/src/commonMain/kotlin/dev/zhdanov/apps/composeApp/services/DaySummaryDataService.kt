package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.DaySummary
import dev.zhdanov.apps.shared.model.TaskSummary
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DaySummaryDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers,
    private val workspaceSessionService: WorkspaceSessionService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun addDaySummary(daySummary: DaySummary) = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.addDaySummaryRaw(
            date = daySummary.date,
            focusTime = daySummary.focusTime,
            review = workspaceSessionService.encryptTextForCurrentWorkspace(daySummary.review),
            linkedTasks = workspaceSessionService.encryptTextForCurrentWorkspace(json.encodeToString(daySummary.linkedTasks)),
            workspaceId = workspaceSessionService.requireCurrentWorkspaceId()
        )
    }

    suspend fun getAllDaySummaries(): List<DaySummary> = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.getAllDaySummaryRecords(workspaceSessionService.requireCurrentWorkspaceId())
            .map { record ->
                DaySummary(
                    date = record.date,
                    focusTime = record.focusTime,
                    review = workspaceSessionService.decryptTextForCurrentWorkspace(record.review),
                    linkedTasks = decodeLinkedTasks(record.linkedTasks),
                    workspaceId = record.workspaceId,
                    syncId = record.syncId,
                    updatedAt = record.updatedAt,
                    deletedAt = record.deletedAt
                )
            }
    }

    suspend fun getDaySummary(date: LocalDate): DaySummary? = withContext(dispatchers.io) {
        workspaceSessionService.requireUnlockedForCurrentWorkspace()
        database.getDaySummaryRecord(date, workspaceSessionService.requireCurrentWorkspaceId())
            ?.let { record ->
                DaySummary(
                    date = record.date,
                    focusTime = record.focusTime,
                    review = workspaceSessionService.decryptTextForCurrentWorkspace(record.review),
                    linkedTasks = decodeLinkedTasks(record.linkedTasks),
                    workspaceId = record.workspaceId,
                    syncId = record.syncId,
                    updatedAt = record.updatedAt,
                    deletedAt = record.deletedAt
                )
            }
    }

    private fun decodeLinkedTasks(value: String): List<TaskSummary> {
        val decrypted = workspaceSessionService.decryptTextForCurrentWorkspace(value)
        return runCatching {
            json.decodeFromString<List<TaskSummary>>(decrypted)
        }.getOrDefault(emptyList())
    }
}
