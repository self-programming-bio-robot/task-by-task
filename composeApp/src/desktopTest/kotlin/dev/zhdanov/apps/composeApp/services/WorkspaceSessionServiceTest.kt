package dev.zhdanov.apps.composeApp.services

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.cache.DatabaseDriverFactory
import dev.zhdanov.apps.shared.model.DEFAULT_ASSISTANT_BASE_URL
import dev.zhdanov.apps.shared.model.DEFAULT_WORKSPACE_ID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceSessionServiceTest {
    @Test
    fun `encrypted workspace stores task text encrypted and reads it after unlock`() = runTest {
        val fixture = createFixture()

        fixture.workspace.enableEncryption("1234")
        fixture.tasks.addTask("Secret task")

        val rawTask = fixture.database.taskRepository.getAllTasks(DEFAULT_WORKSPACE_ID).first()
        assertNotEquals("Secret task", rawTask.title)
        assertEquals("Secret task", fixture.tasks.getAllTasks().first().title)

        fixture.workspace.lockCurrentWorkspace()
        assertFailsWith<WorkspaceLockedException> {
            fixture.tasks.getAllTasks()
        }
        assertFailsWith<InvalidWorkspacePinException> {
            fixture.workspace.unlockCurrentWorkspace("0000")
        }

        fixture.workspace.unlockCurrentWorkspace("1234")
        assertEquals("Secret task", fixture.tasks.getAllTasks().first().title)
    }

    @Test
    fun `disabling encryption decrypts local fields back to plaintext`() = runTest {
        val fixture = createFixture()

        fixture.workspace.enableEncryption("1234")
        fixture.tasks.addTask("Plain again")
        fixture.workspace.disableEncryption("1234")

        val rawTask = fixture.database.taskRepository.getAllTasks(DEFAULT_WORKSPACE_ID).first()
        assertEquals("Plain again", rawTask.title)
        assertFalse(
            fixture.database.workspaceRepository
                .getSecuritySettings(DEFAULT_WORKSPACE_ID)!!
                .encryptionEnabled
        )
    }

    @Test
    fun `assistant config is stored per workspace with custom model and host`() = runTest {
        val fixture = createFixture()
        val second = fixture.workspace.createWorkspace("Local model")

        fixture.workspace.saveAssistantConfig("default-token", DEFAULT_ASSISTANT_BASE_URL, "gpt-4.1")
        fixture.workspace.selectWorkspace(second.id)
        fixture.workspace.saveAssistantConfig("local-token", "http://localhost:11434/v1/", "llama3.1")

        assertEquals("local-token", fixture.workspace.getAssistantConfig()?.token)
        assertEquals("llama3.1", fixture.workspace.getAssistantConfig()?.modelId)
        assertEquals("http://localhost:11434/v1/", fixture.workspace.getAssistantConfig()?.baseUrl)

        fixture.workspace.selectWorkspace(DEFAULT_WORKSPACE_ID)
        assertEquals("default-token", fixture.workspace.getAssistantConfig()?.token)
    }

    @Test
    fun `fresh workspace starts unlocked and unencrypted`() {
        val fixture = createFixture()

        assertFalse(fixture.workspace.isCurrentWorkspaceLocked.value)
        assertTrue(fixture.workspace.securitySettings.value?.encryptionEnabled == false)
    }

    private fun createFixture(): WorkspaceFixture {
        val database = Database(WorkspaceInMemoryDriverFactory())
        val workspace = createWorkspaceSessionService(database)
        val dispatchers = AppDispatchers(
            io = UnconfinedTestDispatcher(),
            default = UnconfinedTestDispatcher()
        )
        return WorkspaceFixture(
            database = database,
            workspace = workspace,
            tasks = TaskDataService(database, dispatchers, workspace)
        )
    }
}

private data class WorkspaceFixture(
    val database: Database,
    val workspace: WorkspaceSessionService,
    val tasks: TaskDataService
)

private class WorkspaceInMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
}
