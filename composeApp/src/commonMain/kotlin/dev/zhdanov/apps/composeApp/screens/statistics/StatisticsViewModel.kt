package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.shared.cache.Database
import dev.zhdanov.apps.shared.model.FocusTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

data class ChartEntry(
    val label: String,
    val focusTimeMinutes: Int
)

class StatisticsViewModel(
    private val database: Database
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.DAY)
    val selectedPeriod: StateFlow<StatisticsPeriod> = _selectedPeriod.asStateFlow()

    private val _focusTimeData = MutableStateFlow(0L)
    val focusTimeData: StateFlow<Long> = _focusTimeData.asStateFlow()

    private val _workCyclesData = MutableStateFlow(0)
    val workCyclesData: StateFlow<Int> = _workCyclesData.asStateFlow()

    private val _tasksCreatedData = MutableStateFlow(0)
    val tasksCreatedData: StateFlow<Int> = _tasksCreatedData.asStateFlow()

    private val _tasksDoneData = MutableStateFlow(0)
    val tasksDoneData: StateFlow<Int> = _tasksDoneData.asStateFlow()

    private val _chartData = MutableStateFlow<List<ChartEntry>>(emptyList())
    val chartData: StateFlow<List<ChartEntry>> = _chartData.asStateFlow()

    init {
        loadData()
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val (from, to, startDate) = getDateRange()
            val focusTimes = database.getAllFocusTimesBetween(from, to)

            // Focus time in seconds
            _focusTimeData.value = focusTimes.sumOf { it.duration.toLong() }

            // Work cycles count
            _workCyclesData.value = focusTimes.size

            // Tasks data
            val allTasks = database.taskRepository.getAllTasks()
            _tasksCreatedData.value = allTasks.size
            _tasksDoneData.value = allTasks.count { it.isCompleted }

            // Chart data
            _chartData.value = buildChartData(focusTimes, startDate)
        }
    }

    private fun buildChartData(focusTimes: List<FocusTime>, startDate: LocalDate): List<ChartEntry> {
        val timeZone = TimeZone.currentSystemDefault()

        return when (_selectedPeriod.value) {
            StatisticsPeriod.DAY -> {
                // Group by hour (24 hours)
                val hourData = MutableList(24) { 0 }
                focusTimes.forEach { ft ->
                    val instant = Instant.fromEpochMilliseconds(ft.finishedAt)
                    val dateTime = instant.toLocalDateTime(timeZone)
                    hourData[dateTime.hour] += ft.duration / 60
                }
                hourData.mapIndexed { hour, minutes ->
                    ChartEntry("${hour}:00", minutes)
                }
            }
            StatisticsPeriod.WEEK -> {
                // Group by day of week (7 days)
                val dayData = MutableList(7) { 0 }
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                focusTimes.forEach { ft ->
                    val instant = Instant.fromEpochMilliseconds(ft.finishedAt)
                    val dateTime = instant.toLocalDateTime(timeZone)
                    // dayOfWeek.ordinal returns 0 for Monday, 6 for Sunday
                    val dayIndex = dateTime.dayOfWeek.ordinal
                    dayData[dayIndex] += ft.duration / 60
                }
                dayData.mapIndexed { index, minutes ->
                    ChartEntry(dayNames[index], minutes)
                }
            }
            StatisticsPeriod.MONTH -> {
                // Group by day of month (show first 4 weeks = 28 days for simplicity)
                val dayData = MutableList(28) { 0 }
                focusTimes.forEach { ft ->
                    val instant = Instant.fromEpochMilliseconds(ft.finishedAt)
                    val dateTime = instant.toLocalDateTime(timeZone)
                    val dayOfMonth = dateTime.dayOfMonth - 1
                    if (dayOfMonth in 0 until 28) {
                        dayData[dayOfMonth] += ft.duration / 60
                    }
                }
                dayData.mapIndexed { index, minutes ->
                    ChartEntry("${index + 1}", minutes)
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getDateRange(): Triple<Long, Long, LocalDate> {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val currentDateTime = now.toLocalDateTime(timeZone)
        val today = currentDateTime.date

        val fromDate = when (_selectedPeriod.value) {
            StatisticsPeriod.DAY -> today
            StatisticsPeriod.WEEK -> today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY)
            StatisticsPeriod.MONTH -> LocalDate(today.year, today.month, 1)
        }

        val fromInstant = LocalDateTime(fromDate, LocalTime(0, 0))
            .toInstant(timeZone)

        return Triple(fromInstant.toEpochMilliseconds(), now.toEpochMilliseconds(), fromDate)
    }
}
