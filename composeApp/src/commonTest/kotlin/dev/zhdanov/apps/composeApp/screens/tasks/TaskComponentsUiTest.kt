package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import dev.zhdanov.apps.composeApp.screens.home.TodayTaskItem
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.model.Task
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TaskComponentsUiTest {
    @Test
    fun `new task input submits trimmed task and clears after submit`() = runComposeUiTest {
        val submissions = mutableListOf<String>()

        setContent {
            MaterialTheme {
                NewTaskInput(onAddTask = submissions::add)
            }
        }

        onNodeWithTag(UiTestTags.NewTaskInput).performTextInput("  Write UI tests  ")
        onNodeWithTag(UiTestTags.AddTaskButton).performClick()
        onNodeWithTag(UiTestTags.AddTaskButton).performClick()

        assertEquals(listOf("Write UI tests"), submissions)
    }

    @Test
    fun `new task input ignores blank submissions`() = runComposeUiTest {
        val submissions = mutableListOf<String>()

        setContent {
            MaterialTheme {
                NewTaskInput(onAddTask = submissions::add)
            }
        }

        onNodeWithTag(UiTestTags.NewTaskInput).performTextInput("   ")
        onNodeWithTag(UiTestTags.AddTaskButton).performClick()

        assertEquals(emptyList(), submissions)
    }

    @Test
    fun `task item exposes row and action callbacks`() = runComposeUiTest {
        val task = task(id = 7, title = "Plan release")
        var completionClicks = 0
        var focusClicks = 0
        var rowClicks = 0
        val todayValues = mutableListOf<Boolean>()

        setContent {
            MaterialTheme {
                TaskItem(
                    task = task,
                    isFocused = false,
                    onToggleCompletion = { completionClicks += 1 },
                    onAddToday = todayValues::add,
                    onFocusToggle = { focusClicks += 1 },
                    onClick = { rowClicks += 1 }
                )
            }
        }

        onNodeWithTag(UiTestTags.taskRow(task.id)).performClick()
        onNodeWithTag(UiTestTags.taskCompletion(task.id)).performClick()
        onNodeWithTag(UiTestTags.taskFocus(task.id)).assertIsEnabled().performClick()
        onNodeWithTag(UiTestTags.taskToday(task.id)).performClick()

        assertEquals(1, rowClicks)
        assertEquals(1, completionClicks)
        assertEquals(1, focusClicks)
        assertEquals(listOf(true), todayValues)
    }

    @Test
    fun `task item disables focus for completed non focused task`() = runComposeUiTest {
        val task = task(id = 8, title = "Done task", isCompleted = true)

        setContent {
            MaterialTheme {
                TaskItem(
                    task = task,
                    isFocused = false,
                    onToggleCompletion = {},
                    onAddToday = {},
                    onFocusToggle = {},
                    onClick = {}
                )
            }
        }

        onNodeWithTag(UiTestTags.taskFocus(task.id)).assertIsNotEnabled()
    }

    @Test
    fun `today task item exposes row completion and focus callbacks`() = runComposeUiTest {
        val task = task(id = 9, title = "Today work", isToday = true)
        var completionClicks = 0
        var focusClicks = 0
        var rowClicks = 0

        setContent {
            MaterialTheme {
                TodayTaskItem(
                    task = task,
                    isFocused = false,
                    isTimerRunning = false,
                    onToggleCompletion = { completionClicks += 1 },
                    onFocusToggle = { focusClicks += 1 },
                    onClick = { rowClicks += 1 }
                )
            }
        }

        onNodeWithTag(UiTestTags.taskRow(task.id)).performClick()
        onNodeWithTag(UiTestTags.taskCompletion(task.id)).performClick()
        onNodeWithTag(UiTestTags.taskFocus(task.id)).assertIsEnabled().performClick()

        assertEquals(1, rowClicks)
        assertEquals(1, completionClicks)
        assertEquals(1, focusClicks)
    }

    @Test
    fun `today task item disables focus for completed task`() = runComposeUiTest {
        val task = task(id = 10, title = "Finished today", isCompleted = true, isToday = true)

        setContent {
            MaterialTheme {
                TodayTaskItem(
                    task = task,
                    isFocused = false,
                    isTimerRunning = false,
                    onToggleCompletion = {},
                    onFocusToggle = {},
                    onClick = {}
                )
            }
        }

        onNodeWithTag(UiTestTags.taskFocus(task.id)).assertIsNotEnabled()
    }

    private fun task(
        id: Long,
        title: String,
        isCompleted: Boolean = false,
        isToday: Boolean = false
    ) = Task(
        id = id,
        title = title,
        createdAt = LocalDateTime(2026, 5, 16, 9, 0),
        isCompleted = isCompleted,
        isToday = isToday
    )
}
