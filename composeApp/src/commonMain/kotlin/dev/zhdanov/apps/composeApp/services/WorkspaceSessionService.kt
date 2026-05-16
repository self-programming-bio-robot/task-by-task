package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.AssistantConfig
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_BASE_URL
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_MODEL
import dev.zhdanov.apps.shared.model.DEFAULT_ENCRYPTION_ITERATIONS
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import dev.zhdanov.apps.shared.model.Workspace
import dev.zhdanov.apps.shared.model.WorkspaceSecuritySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceSessionService(
    private val database: Database,
    private val crypto: WorkspaceCryptoService
) {
    private val unlockedKeys = mutableMapOf<Long, ByteArray>()

    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _currentWorkspace = MutableStateFlow<Workspace?>(null)
    val currentWorkspace: StateFlow<Workspace?> = _currentWorkspace.asStateFlow()

    private val _securitySettings = MutableStateFlow<WorkspaceSecuritySettings?>(null)
    val securitySettings: StateFlow<WorkspaceSecuritySettings?> = _securitySettings.asStateFlow()

    private val _isCurrentWorkspaceLocked = MutableStateFlow(false)
    val isCurrentWorkspaceLocked: StateFlow<Boolean> = _isCurrentWorkspaceLocked.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        database.workspaceRepository.ensureDefaultWorkspace()
        _workspaces.value = database.workspaceRepository.getWorkspaces()
        _currentWorkspace.value = database.workspaceRepository.getSelectedWorkspace()
            ?: database.workspaceRepository.ensureDefaultWorkspace()
        refreshSecurity()
    }

    fun requireCurrentWorkspaceId(): Long =
        _currentWorkspace.value?.id ?: database.workspaceRepository.ensureDefaultWorkspace().id

    fun createWorkspace(name: String): Workspace {
        val workspace = database.workspaceRepository.createWorkspace(name)
        reload()
        return workspace
    }

    fun renameCurrentWorkspace(name: String) {
        val workspaceId = requireCurrentWorkspaceId()
        database.workspaceRepository.renameWorkspace(workspaceId, name)
        reload()
    }

    fun updateCurrentWorkspaceIcon(icon: String) {
        val workspaceId = requireCurrentWorkspaceId()
        database.workspaceRepository.updateWorkspaceIcon(workspaceId, icon)
        reload()
    }

    fun selectWorkspace(id: Long) {
        database.workspaceRepository.selectWorkspace(id)
        reload()
    }

    fun lockCurrentWorkspace() {
        unlockedKeys.remove(requireCurrentWorkspaceId())
        refreshLockState()
    }

    fun unlockCurrentWorkspace(pin: String) {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        if (!settings.encryptionEnabled) {
            refreshLockState()
            return
        }
        val salt = settings.encryptionSalt ?: throw WorkspaceLockedException()
        val wrappedKey = settings.wrappedDataKey ?: throw WorkspaceLockedException()
        unlockedKeys[workspaceId] = crypto.unwrapDataKey(
            pin = pin,
            salt = salt,
            iterations = settings.encryptionIterations,
            wrappedDataKey = wrappedKey
        )
        refreshLockState()
    }

    fun enableEncryption(pin: String) {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        if (settings.encryptionEnabled) {
            unlockCurrentWorkspace(pin)
            return
        }

        val dataKey = crypto.generateDataKey()
        val salt = crypto.generateSalt()
        val iterations = DEFAULT_ENCRYPTION_ITERATIONS
        val wrappedKey = crypto.wrapDataKey(pin, salt, iterations, dataKey)

        database.transaction {
            transformWorkspaceFields(workspaceId, dataKey, encrypt = true)
            database.workspaceRepository.updateEncryption(
                workspaceId = workspaceId,
                enabled = true,
                salt = salt,
                wrappedDataKey = wrappedKey,
                iterations = iterations
            )
        }

        unlockedKeys[workspaceId] = dataKey
        reload()
    }

    fun disableEncryption(pin: String) {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        if (!settings.encryptionEnabled) {
            return
        }

        unlockCurrentWorkspace(pin)
        val dataKey = requireUnlockedKey(workspaceId)
        database.transaction {
            transformWorkspaceFields(workspaceId, dataKey, encrypt = false)
            database.workspaceRepository.updateEncryption(
                workspaceId = workspaceId,
                enabled = false,
                salt = null,
                wrappedDataKey = null,
                iterations = DEFAULT_ENCRYPTION_ITERATIONS
            )
        }

        unlockedKeys.remove(workspaceId)
        reload()
    }

    fun saveAssistantConfig(token: String, baseUrl: String, modelId: String) {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        val storedToken = if (settings.encryptionEnabled) {
            encryptText(workspaceId, token)
        } else {
            token
        }

        database.workspaceRepository.updateAssistantConfig(
            workspaceId = workspaceId,
            token = storedToken,
            baseUrl = baseUrl.ifBlank { DEFAULT_ASSISTANT_BASE_URL },
            modelId = modelId.ifBlank { DEFAULT_ASSISTANT_MODEL }
        )
        refreshSecurity()
    }

    fun getAssistantConfig(): AssistantConfig? {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        val token = decryptText(workspaceId, settings.openAiToken).takeIf { it.isNotBlank() } ?: return null
        return AssistantConfig(
            token = token,
            modelId = settings.llmModelId.ifBlank { DEFAULT_ASSISTANT_MODEL },
            baseUrl = settings.llmBaseUrl.ifBlank { DEFAULT_ASSISTANT_BASE_URL }
        )
    }

    fun getAssistantConfigForUi(): AssistantConfig {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        return AssistantConfig(
            token = decryptText(workspaceId, settings.openAiToken),
            modelId = settings.llmModelId.ifBlank { DEFAULT_ASSISTANT_MODEL },
            baseUrl = settings.llmBaseUrl.ifBlank { DEFAULT_ASSISTANT_BASE_URL }
        )
    }

    fun encryptTextForCurrentWorkspace(value: String): String =
        encryptText(requireCurrentWorkspaceId(), value)

    fun encryptNullableTextForCurrentWorkspace(value: String?): String? =
        value?.let(::encryptTextForCurrentWorkspace)

    fun decryptTextForCurrentWorkspace(value: String): String =
        decryptText(requireCurrentWorkspaceId(), value)

    fun decryptNullableTextForCurrentWorkspace(value: String?): String? =
        value?.let(::decryptTextForCurrentWorkspace)

    fun requireUnlockedForCurrentWorkspace() {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = currentSecuritySettings()
        if (settings.encryptionEnabled && unlockedKeys[workspaceId] == null) {
            throw WorkspaceLockedException()
        }
    }

    private fun currentSecuritySettings(): WorkspaceSecuritySettings {
        val workspaceId = requireCurrentWorkspaceId()
        return database.workspaceRepository.getSecuritySettings(workspaceId)
            ?: database.workspaceRepository.ensureSecuritySettings(workspaceId)
    }

    private fun refreshSecurity() {
        _securitySettings.value = currentSecuritySettings()
        refreshLockState()
    }

    private fun refreshLockState() {
        val workspaceId = requireCurrentWorkspaceId()
        val settings = database.workspaceRepository.getSecuritySettings(workspaceId)
        _isCurrentWorkspaceLocked.value = settings?.encryptionEnabled == true && unlockedKeys[workspaceId] == null
    }

    private fun encryptText(workspaceId: Long, value: String): String {
        val settings = currentSecuritySettings()
        if (!settings.encryptionEnabled || value.isEmpty() || crypto.isEncrypted(value)) {
            return value
        }
        return crypto.encryptText(value, requireUnlockedKey(workspaceId))
    }

    private fun decryptText(workspaceId: Long, value: String): String {
        val settings = database.workspaceRepository.getSecuritySettings(workspaceId)
            ?: return value
        if (!settings.encryptionEnabled || value.isEmpty() || !crypto.isEncrypted(value)) {
            return value
        }
        return crypto.decryptText(value, requireUnlockedKey(workspaceId))
    }

    private fun requireUnlockedKey(workspaceId: Long): ByteArray =
        unlockedKeys[workspaceId] ?: throw WorkspaceLockedException()

    private fun transformWorkspaceFields(workspaceId: Long, dataKey: ByteArray, encrypt: Boolean) {
        database.taskRepository.getAllTasks(workspaceId).forEach { task ->
            database.updateTaskEncryptedFields(
                id = task.id,
                title = transformText(task.title, dataKey, encrypt),
                description = task.description?.let { transformText(it, dataKey, encrypt) },
                workspaceId = workspaceId
            )
        }

        database.getAllFocusTimes(workspaceId).forEach { focusTime ->
            database.updateFocusTimeFeedback(
                id = focusTime.id,
                feedback = transformText(focusTime.feedback, dataKey, encrypt),
                workspaceId = workspaceId
            )
        }

        database.getAllDaySummaryRecords(workspaceId).forEach { summary ->
            database.updateDaySummaryEncryptedFields(
                date = summary.date,
                review = transformText(summary.review, dataKey, encrypt),
                linkedTasks = transformText(summary.linkedTasks, dataKey, encrypt),
                workspaceId = workspaceId
            )
        }

        val settings = database.workspaceRepository.getSecuritySettings(workspaceId)
        if (settings != null) {
            database.workspaceRepository.updateAssistantConfig(
                workspaceId = workspaceId,
                token = transformText(settings.openAiToken, dataKey, encrypt),
                baseUrl = settings.llmBaseUrl.ifBlank { DEFAULT_ASSISTANT_BASE_URL },
                modelId = settings.llmModelId.ifBlank { DEFAULT_ASSISTANT_MODEL }
            )
        }
    }

    private fun transformText(value: String, dataKey: ByteArray, encrypt: Boolean): String {
        if (value.isEmpty()) return value
        return when {
            encrypt && crypto.isEncrypted(value) -> value
            encrypt -> crypto.encryptText(value, dataKey)
            !encrypt && crypto.isEncrypted(value) -> crypto.decryptText(value, dataKey)
            else -> value
        }
    }
}
