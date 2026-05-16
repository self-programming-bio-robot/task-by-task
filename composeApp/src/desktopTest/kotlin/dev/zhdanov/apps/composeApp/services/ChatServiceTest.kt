package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.model.AssistantConfig
import dev.zhdanov.apps.shared.model.ChatMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatServiceTest {
    @Test
    fun `sendMessage returns failure when token is missing`() = runTest {
        val service = createChatService()
        service.startSession(LocalDate(2026, 5, 15), "summary")

        val result = service.sendMessage("hello")

        assertTrue(result.isFailure)
        assertEquals("OpenAI token not found", service.error.value)
    }

    @Test
    fun `sendMessage appends assistant response through injected client`() = runTest {
        val database = Database(ChatInMemoryDriverFactory())
        val dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher()
        )
        val workspace = createWorkspaceSessionService(database)
        val settings = AppSettingsService(database, dispatchers, workspace)
        settings.saveAssistantConfig("token", "https://api.openai.com/v1/", "gpt-4.1")
        val service = ChatService(workspace, NotificationService(), FakeChatClient())
        service.startSession(LocalDate(2026, 5, 15), "summary")

        val result = service.sendMessage("hello")

        assertTrue(result.isSuccess)
        assertEquals("fake reply", result.getOrThrow().content)
        assertEquals(2, service.currentSession.value?.messages?.size)
    }

    private fun createChatService(): ChatService {
        val database = Database(ChatInMemoryDriverFactory())
        val dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher()
        )
        return ChatService(
            workspaceSessionService = createWorkspaceSessionService(database),
            notificationService = NotificationService(),
            chatClient = FakeChatClient()
        )
    }
}

private class FakeChatClient : ChatClient {
    override suspend fun sendMessage(config: AssistantConfig, daySummary: String, messages: List<ChatMessage>): String {
        return "fake reply"
    }
}

private class ChatInMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}
