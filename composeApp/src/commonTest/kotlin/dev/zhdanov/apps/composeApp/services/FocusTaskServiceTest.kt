package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusTaskServiceTest {

    @Test
    fun `setFocusedTask updates focused task`() = runTest {
        // Given
        val service = FocusTaskService()
        val task = Task(
            id = 1,
            title = "Test Task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )

        // When
        service.setFocusedTask(task)

        // Then
        assertEquals(task, service.focusedTask.first())
    }

    @Test
    fun `setFocusedTask to null clears focused task`() = runTest {
        // Given
        val service = FocusTaskService()
        val task = Task(
            id = 1,
            title = "Test Task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
        service.setFocusedTask(task)

        // When
        service.setFocusedTask(null)

        // Then
        assertNull(service.focusedTask.first())
    }

    @Test
    fun `clearFocusedTask clears focused task`() = runTest {
        // Given
        val service = FocusTaskService()
        val task = Task(
            id = 1,
            title = "Test Task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
        service.setFocusedTask(task)

        // When
        service.clearFocusedTask()

        // Then
        assertNull(service.focusedTask.first())
    }

    @Test
    fun `getFocusedTaskId returns correct id`() = runTest {
        // Given
        val service = FocusTaskService()
        val task = Task(
            id = 42,
            title = "Test Task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
        service.setFocusedTask(task)

        // When
        val taskId = service.getFocusedTaskId()

        // Then
        assertEquals(42L, taskId)
    }

    @Test
    fun `getFocusedTaskId returns null when no task is focused`() = runTest {
        // Given
        val service = FocusTaskService()

        // When
        val taskId = service.getFocusedTaskId()

        // Then
        assertNull(taskId)
    }

    @Test
    fun `focused task is initially null`() = runTest {
        // Given
        val service = FocusTaskService()

        // When
        val focusedTask = service.focusedTask.first()

        // Then
        assertNull(focusedTask)
    }

    @Test
    fun `switching focused task replaces previous task`() = runTest {
        // Given
        val service = FocusTaskService()
        val task1 = Task(
            id = 1,
            title = "First Task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
        val task2 = Task(
            id = 2,
            title = "Second Task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
        service.setFocusedTask(task1)

        // When
        service.setFocusedTask(task2)

        // Then
        assertEquals(task2, service.focusedTask.first())
        assertEquals(2L, service.getFocusedTaskId())
    }

    @Test
    fun `selectTask focuses task when timer is not running`() = runTest {
        val service = FocusTaskService()
        val task = Task(
            id = 1,
            title = "Selectable task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )

        val result = service.selectTask(task, isTimerRunning = false)

        assertTrue(result)
        assertEquals(task, service.focusedTask.first())
        assertEquals(listOf(1L), service.getAllTaskIdsForSession())
    }

    @Test
    fun `selectTask blocks switching while timer is running and current task is incomplete`() = runTest {
        val service = FocusTaskService()
        val currentTask = Task(
            id = 1,
            title = "Current task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
        val nextTask = Task(
            id = 2,
            title = "Next task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 1)
        )
        service.setFocusedTask(currentTask)

        val result = service.selectTask(nextTask, isTimerRunning = true)

        assertFalse(result)
        assertEquals(currentTask, service.focusedTask.first())
        assertEquals(listOf(1L), service.getAllTaskIdsForSession())
    }

    @Test
    fun `selectTask moves completed current task into session history before switching`() = runTest {
        val service = FocusTaskService()
        val completedTask = Task(
            id = 1,
            title = "Completed task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0),
            isCompleted = true
        )
        val nextTask = Task(
            id = 2,
            title = "Next task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 1)
        )
        service.setFocusedTask(completedTask)

        val result = service.selectTask(nextTask, isTimerRunning = true)

        assertTrue(result)
        assertEquals(nextTask, service.focusedTask.first())
        assertEquals(listOf(1L, 2L), service.getAllTaskIdsForSession())
        assertEquals(listOf(completedTask, nextTask), service.getAllTasksForSession())
    }

    @Test
    fun `toggleTaskSelection deselects completed focused task and preserves it in session`() = runTest {
        val service = FocusTaskService()
        val completedTask = Task(
            id = 3,
            title = "Done task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0),
            isCompleted = true
        )
        service.setFocusedTask(completedTask)

        val result = service.toggleTaskSelection(completedTask, isTimerRunning = true)

        assertTrue(result)
        assertNull(service.focusedTask.first())
        assertEquals(listOf(3L), service.getAllTaskIdsForSession())
        assertEquals(listOf(completedTask), service.completedTasks.first())
    }

    @Test
    fun `clearFocusedTask clears completed session tasks`() = runTest {
        val service = FocusTaskService()
        val completedTask = Task(
            id = 4,
            title = "Completed task",
            createdAt = LocalDateTime(2024, 1, 1, 0, 0),
            isCompleted = true
        )
        service.setFocusedTask(completedTask)
        service.toggleTaskSelection(completedTask, isTimerRunning = true)

        service.clearFocusedTask()

        assertNull(service.focusedTask.first())
        assertEquals(emptyList(), service.completedTasks.first())
        assertEquals(emptyList(), service.getAllTaskIdsForSession())
    }
}
