package dev.zhdanov.apps.composeApp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.zhdanov.apps.composeApp.App
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.cache.Database
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class AppDesktopSmokeUiTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var database: Database
    private lateinit var focusTaskService: FocusTaskService

    @Before
    fun setUp() {
        startDesktopUiTestKoin()
        database = GlobalContext.get().get()
        focusTaskService = GlobalContext.get().get()
    }

    @After
    fun tearDown() {
        stopDesktopUiTestKoin()
    }

    @Test
    fun appNavigatesToTasksAddsTaskAndUpdatesTaskState() {
        rule.setContent {
            App()
        }

        rule.onNodeWithTag(UiTestTags.AppRoot).assertExists()
        waitForTag(UiTestTags.HomeScreen)

        rule.onNodeWithTag(UiTestTags.navigationItem("Tasks")).performClick()
        waitForTag(UiTestTags.TaskListScreen)

        rule.onNodeWithTag(UiTestTags.NewTaskInput).performTextInput("Smoke task")
        rule.onNodeWithTag(UiTestTags.AddTaskButton).performClick()

        val taskId = waitForTaskId("Smoke task")
        waitForText("Smoke task")

        rule.onNodeWithTag(UiTestTags.taskToday(taskId)).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            database.taskRepository.getTaskById(taskId)?.isToday == true
        }

        rule.onNodeWithTag(UiTestTags.taskFocus(taskId)).performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            focusTaskService.focusedTask.value?.id == taskId
        }
        waitForTag(UiTestTags.HomeScreen)
    }

    private fun waitForTag(tag: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                rule.onNodeWithTag(tag).assertExists()
            }.isSuccess
        }
    }

    private fun waitForText(text: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTaskId(title: String): Long {
        rule.waitUntil(timeoutMillis = 5_000) {
            database.taskRepository.getAllTasks().any { it.title == title } &&
                rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }

        return database.taskRepository.getAllTasks().single { it.title == title }.id
    }
}
