package dev.zhdanov.apps.shared.model

import kotlinx.serialization.Serializable

const val DEFAULT_WORKSPACE_ID: Long = 1L
const val DEFAULT_WORKSPACE_NAME = "Local workspace"
const val DEFAULT_WORKSPACE_ICON = "workspaces"
const val DEFAULT_ASSISTANT_MODEL = "gpt-4.1"
const val DEFAULT_ASSISTANT_BASE_URL = "https://api.openai.com/v1/"
const val DEFAULT_ENCRYPTION_ITERATIONS = 600_000

@Serializable
data class Workspace(
    val id: Long,
    val syncId: String,
    val name: String,
    val icon: String = DEFAULT_WORKSPACE_ICON,
    val isSelected: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Serializable
data class WorkspaceSecuritySettings(
    val workspaceId: Long,
    val openAiToken: String = "",
    val llmBaseUrl: String = DEFAULT_ASSISTANT_BASE_URL,
    val llmModelId: String = DEFAULT_ASSISTANT_MODEL,
    val encryptionEnabled: Boolean = false,
    val encryptionSalt: String? = null,
    val wrappedDataKey: String? = null,
    val encryptionIterations: Int = DEFAULT_ENCRYPTION_ITERATIONS
)

data class AssistantConfig(
    val token: String,
    val modelId: String = DEFAULT_ASSISTANT_MODEL,
    val baseUrl: String = DEFAULT_ASSISTANT_BASE_URL
)
