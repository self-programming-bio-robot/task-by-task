package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.DaySummary
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

class DaySummaryDataService(
    private val database: Database,
    private val dispatchers: AppDispatchers
) {
    suspend fun addDaySummary(daySummary: DaySummary) = withContext(dispatchers.io) {
        database.addDaySummary(daySummary)
    }

    suspend fun getAllDaySummaries(): List<DaySummary> = withContext(dispatchers.io) {
        database.getAllDaySummaries()
    }

    suspend fun getDaySummary(date: LocalDate): DaySummary? = withContext(dispatchers.io) {
        database.getDaySummary(date)
    }
}
