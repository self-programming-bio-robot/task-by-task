package dev.zhdanov.apps.composeApp.screens.tasks

import dev.zhdanov.apps.shared.model.Task
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
enum class TaskTypeFilter {
    REGULAR,
    ROUTINE,
}

@Serializable
data class TaskListFilter(
    val showCompleted: Boolean = false,
    val taskTypes: Set<TaskTypeFilter> = TaskTypeFilter.entries.toSet(),
    val todayOnly: Boolean = false,
    val overdueOnly: Boolean = false,
) {
    val isDefault: Boolean
        get() = !showCompleted &&
            taskTypes.size == TaskTypeFilter.entries.size &&
            !todayOnly &&
            !overdueOnly
}

data class TaskGroup(
    val date: Long?,
    val tasks: List<Task>,
)

fun Task.isDueOn(day: LocalDate): Boolean = targetDate == day.toLong()

fun Task.isOverdueOn(day: LocalDate): Boolean {
    val due = targetDate ?: return false
    return !isCompleted && due < day.toLong()
}

// TODO: implement filtering in db query
fun List<Task>.applyFilter(
    filter: TaskListFilter,
    query: String,
    today: LocalDate,
): List<TaskGroup> {
    val todayLong = today.toLong()
    val trimmedQuery = query.trim()

    val filtered = filter { task ->
        if (!filter.showCompleted && task.isCompleted) return@filter false

        val type = if (task.routineId != null) TaskTypeFilter.ROUTINE else TaskTypeFilter.REGULAR
        if (type !in filter.taskTypes) return@filter false

        if (filter.todayOnly || filter.overdueOnly) {
            val matchesToday = filter.todayOnly && task.targetDate == todayLong
            val matchesOverdue = filter.overdueOnly && task.isOverdueOn(today)
            if (!matchesToday && !matchesOverdue) return@filter false
        }

        if (trimmedQuery.isNotEmpty()) {
            val matchesTitle = task.title.contains(trimmedQuery, ignoreCase = true)
            val matchesDesc = task.description?.contains(trimmedQuery, ignoreCase = true) == true
            if (!matchesTitle && !matchesDesc) return@filter false
        }

        true
    }

    val taskComparator = compareBy<Task>({ it.isCompleted })
        .thenByDescending { it.createdAt }

    return filtered
        .groupBy { it.targetDate }
        .map { (date, tasks) -> TaskGroup(date, tasks.sortedWith(taskComparator)) }
        .sortedWith { a, b -> compareGroupDates(a.date, b.date, todayLong) }
}

private fun compareGroupDates(a: Long?, b: Long?, todayLong: Long): Int {
    if (a == b) return 0
    val bucketA = groupBucket(a, todayLong)
    val bucketB = groupBucket(b, todayLong)
    if (bucketA != bucketB) return bucketA.compareTo(bucketB)
    // Inside future (bucket 1) and past (bucket 2), sort newest to oldest
    return (b ?: 0L).compareTo(a ?: 0L)
}

private fun groupBucket(date: Long?, todayLong: Long): Int = when {
    date == null -> 3
    date == todayLong -> 0
    date > todayLong -> 1
    else -> 2
}
