package dev.zhdanov.apps.composeApp.screens.tasks

import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TaskListFilterTest {
    private val today = LocalDate(2026, 5, 16)

    @Test
    fun `default filter hides completed tasks and groups by today then future desc then past desc then null`() {
        val tasks = listOf(
            task(1, "No date", targetDate = null),
            task(2, "Past old", targetDate = LocalDate(2026, 5, 10).toLong()),
            task(3, "Past recent", targetDate = LocalDate(2026, 5, 15).toLong()),
            task(4, "Today task", targetDate = today.toLong()),
            task(5, "Future near", targetDate = LocalDate(2026, 5, 17).toLong()),
            task(6, "Future far", targetDate = LocalDate(2026, 5, 25).toLong()),
            task(7, "Completed today", targetDate = today.toLong(), isCompleted = true),
        )

        val groups = tasks.applyFilter(TaskListFilter(), query = "", today = today)

        assertEquals(
            listOf(
                today.toLong(),
                LocalDate(2026, 5, 25).toLong(),
                LocalDate(2026, 5, 17).toLong(),
                LocalDate(2026, 5, 15).toLong(),
                LocalDate(2026, 5, 10).toLong(),
                null,
            ),
            groups.map { it.date }
        )
        assertEquals(listOf(4L), groups.first().tasks.map { it.id })
    }

    @Test
    fun `showCompleted places completed tasks after unfinished inside each group`() {
        val tasks = listOf(
            task(1, "Done early", targetDate = today.toLong(), isCompleted = true, minute = 30),
            task(2, "Open older", targetDate = today.toLong(), isCompleted = false, minute = 10),
            task(3, "Open newer", targetDate = today.toLong(), isCompleted = false, minute = 20),
        )

        val groups = tasks.applyFilter(
            TaskListFilter(showCompleted = true),
            query = "",
            today = today
        )

        assertEquals(listOf(3L, 2L, 1L), groups.single().tasks.map { it.id })
    }

    @Test
    fun `task type filter supports regular only routine only and empty selection`() {
        val tasks = listOf(
            task(1, "Regular", routineId = null),
            task(2, "Routine", routineId = 99L),
        )

        val regularOnly = tasks.applyFilter(
            TaskListFilter(taskTypes = setOf(TaskTypeFilter.REGULAR)),
            query = "",
            today = today
        )
        assertEquals(listOf(1L), regularOnly.flatMap { it.tasks }.map { it.id })

        val routineOnly = tasks.applyFilter(
            TaskListFilter(taskTypes = setOf(TaskTypeFilter.ROUTINE)),
            query = "",
            today = today
        )
        assertEquals(listOf(2L), routineOnly.flatMap { it.tasks }.map { it.id })

        val none = tasks.applyFilter(
            TaskListFilter(taskTypes = emptySet()),
            query = "",
            today = today
        )
        assertEquals(emptyList(), none)
    }

    @Test
    fun `todayOnly filter keeps strictly today tasks and excludes overdue`() {
        val tasks = listOf(
            task(1, "Today", targetDate = today.toLong()),
            task(2, "Yesterday overdue", targetDate = LocalDate(2026, 5, 15).toLong()),
            task(3, "No date", targetDate = null),
        )

        val groups = tasks.applyFilter(
            TaskListFilter(todayOnly = true),
            query = "",
            today = today
        )

        assertEquals(listOf(1L), groups.flatMap { it.tasks }.map { it.id })
    }

    @Test
    fun `overdueOnly filter keeps uncompleted tasks before today and combines with todayOnly`() {
        val tasks = listOf(
            task(1, "Today", targetDate = today.toLong()),
            task(2, "Yesterday overdue", targetDate = LocalDate(2026, 5, 15).toLong()),
            task(3, "Yesterday completed", targetDate = LocalDate(2026, 5, 15).toLong(), isCompleted = true),
            task(4, "Tomorrow", targetDate = LocalDate(2026, 5, 17).toLong()),
            task(5, "No date", targetDate = null),
        )

        val overdueGroups = tasks.applyFilter(
            TaskListFilter(overdueOnly = true, showCompleted = true),
            query = "",
            today = today
        )
        assertEquals(listOf(2L), overdueGroups.flatMap { it.tasks }.map { it.id })

        val todayAndOverdue = tasks.applyFilter(
            TaskListFilter(todayOnly = true, overdueOnly = true),
            query = "",
            today = today
        )
        assertEquals(listOf(1L, 2L), todayAndOverdue.flatMap { it.tasks }.map { it.id })
    }

    @Test
    fun `search query matches title or description case insensitively`() {
        val tasks = listOf(
            task(1, "Buy Milk", description = null),
            task(2, "Errands", description = "Pick up ALMOND milk"),
            task(3, "Workout", description = "Leg day"),
        )

        val groups = tasks.applyFilter(TaskListFilter(), query = "  milk ", today = today)

        assertEquals(setOf(1L, 2L), groups.flatMap { it.tasks }.map { it.id }.toSet())
    }

    private fun task(
        id: Long,
        title: String,
        description: String? = null,
        targetDate: Long? = today.toLong(),
        routineId: Long? = null,
        isCompleted: Boolean = false,
        minute: Int = 0,
    ) = Task(
        id = id,
        title = title,
        description = description,
        createdAt = LocalDateTime(2026, 5, 16, 9, minute),
        isCompleted = isCompleted,
        routineId = routineId,
        targetDate = targetDate,
    )
}
