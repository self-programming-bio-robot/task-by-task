package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
