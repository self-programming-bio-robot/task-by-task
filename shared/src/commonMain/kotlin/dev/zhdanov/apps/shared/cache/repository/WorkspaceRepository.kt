package dev.zhdanov.apps.shared.cache.repository

import dev.zhdanov.apps.shared.cache.AppDatabaseQueries
import dev.zhdanov.apps.shared.cache.workspaceMapper
import dev.zhdanov.apps.shared.cache.workspaceSecuritySettingsMapper
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_BASE_URL
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_MODEL
import dev.zhdanov.apps.shared.model.DEFAULT_ENCRYPTION_ITERATIONS
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ICON
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_NAME
import dev.zhdanov.apps.shared.model.Workspace
import dev.zhdanov.apps.shared.model.WorkspaceSecuritySettings
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class WorkspaceRepository(
    private val database: AppDatabaseQueries
) {
    fun ensureDefaultWorkspace(): Workspace {
        val now = now()
        val existing = database.selectWorkspaceById(DEFAULT_WORKSPACE_ID, workspaceMapper).executeAsOneOrNull()
        if (existing == null) {
            database.insertWorkspace(
                syncId = "local-workspace",
                name = DEFAULT_WORKSPACE_NAME,
                icon = DEFAULT_WORKSPACE_ICON,
                isSelected = true,
                createdAt = now,
                updatedAt = now
            )
        }
        ensureSecuritySettings(DEFAULT_WORKSPACE_ID)
        val selected = getSelectedWorkspace()
        if (selected == null) {
            selectWorkspace(DEFAULT_WORKSPACE_ID)
        }
        return getSelectedWorkspace() ?: getWorkspace(DEFAULT_WORKSPACE_ID)!!
    }

    fun getWorkspaces(): List<Workspace> =
        database.selectWorkspaces(workspaceMapper).executeAsList()

    fun getWorkspace(id: Long): Workspace? =
        database.selectWorkspaceById(id, workspaceMapper).executeAsOneOrNull()

    fun getSelectedWorkspace(): Workspace? =
        database.selectSelectedWorkspace(workspaceMapper).executeAsOneOrNull()

    fun createWorkspace(name: String, icon: String = DEFAULT_WORKSPACE_ICON): Workspace {
        val now = now()
        val createdWorkspaceId = database.transactionWithResult {
            database.insertWorkspace(
                syncId = Uuid.random().toString(),
                name = name.ifBlank { DEFAULT_WORKSPACE_NAME },
                icon = icon.ifBlank { DEFAULT_WORKSPACE_ICON },
                isSelected = false,
                createdAt = now,
                updatedAt = now
            )
            val workspaceId = database.lastInsertRowId().executeAsOne()
            ensureSecuritySettings(workspaceId)
            workspaceId
        }
        return getWorkspace(createdWorkspaceId) ?: error("Created workspace not found: $createdWorkspaceId")
    }

    fun selectWorkspace(id: Long) {
        database.transaction {
            database.clearSelectedWorkspace()
            database.setSelectedWorkspace(updatedAt = now(), id = id)
        }
    }

    fun renameWorkspace(id: Long, name: String) {
        database.updateWorkspaceName(name = name, updatedAt = now(), id = id)
    }

    fun updateWorkspaceIcon(id: Long, icon: String) {
        database.updateWorkspaceIcon(icon = icon.ifBlank { DEFAULT_WORKSPACE_ICON }, updatedAt = now(), id = id)
    }

    fun softDeleteWorkspace(id: Long) {
        val now = now()
        database.softDeleteWorkspace(deletedAt = now, updatedAt = now, id = id)
        if (getSelectedWorkspace() == null) {
            selectWorkspace(DEFAULT_WORKSPACE_ID)
        }
    }

    fun ensureSecuritySettings(workspaceId: Long): WorkspaceSecuritySettings {
        database.insertWorkspaceSecuritySettings(
            workspaceId = workspaceId,
            openAiToken = "",
            llmBaseUrl = DEFAULT_ASSISTANT_BASE_URL,
            llmModelId = DEFAULT_ASSISTANT_MODEL,
            encryptionEnabled = false,
            encryptionSalt = null,
            wrappedDataKey = null,
            encryptionIterations = DEFAULT_ENCRYPTION_ITERATIONS.toLong()
        )
        return getSecuritySettings(workspaceId)!!
    }

    fun getSecuritySettings(workspaceId: Long): WorkspaceSecuritySettings? =
        database.selectWorkspaceSecuritySettings(workspaceId, workspaceSecuritySettingsMapper).executeAsOneOrNull()

    fun updateAssistantConfig(workspaceId: Long, token: String, baseUrl: String, modelId: String) {
        ensureSecuritySettings(workspaceId)
        database.updateWorkspaceAssistantConfig(
            openAiToken = token,
            llmBaseUrl = baseUrl,
            llmModelId = modelId,
            workspaceId = workspaceId
        )
    }

    fun updateEncryption(
        workspaceId: Long,
        enabled: Boolean,
        salt: String?,
        wrappedDataKey: String?,
        iterations: Int
    ) {
        ensureSecuritySettings(workspaceId)
        database.updateWorkspaceEncryption(
            encryptionEnabled = enabled,
            encryptionSalt = salt,
            wrappedDataKey = wrappedDataKey,
            encryptionIterations = iterations.toLong(),
            workspaceId = workspaceId
        )
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
