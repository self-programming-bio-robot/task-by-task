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

    // Store raw data for re-aggregation when column count changes
    private var rawFocusTimes: List<FocusTime> = emptyList()
    private var rawStartDate: LocalDate? = null

    init {
        loadData()
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        loadData()
    }

    /**
     * Update chart data aggregation based on available columns.
     * Call this when window size changes.
     *
     * @param maxColumns Maximum number of columns that can be displayed
     */
    fun updateColumnCount(maxColumns: Int) {
        rawStartDate?.let { startDate ->
            _chartData.value = buildChartData(rawFocusTimes, startDate, maxColumns)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val (from, to, startDate) = getDateRange()
            val focusTimes = database.getAllFocusTimesBetween(from, to)

            // Store raw data for re-aggregation
            rawFocusTimes = focusTimes
            rawStartDate = startDate

            // Focus time in seconds
            _focusTimeData.value = focusTimes.sumOf { it.duration.toLong() }

            // Work cycles count
            _workCyclesData.value = focusTimes.size

            // Tasks data
            val allTasks = database.taskRepository.getAllTasks()
            _tasksCreatedData.value = allTasks.size
            _tasksDoneData.value = allTasks.count { it.isCompleted }

            // Chart data with default column count
            _chartData.value = buildChartData(focusTimes, startDate, getMaxColumnsForPeriod())
        }
    }

    private fun getMaxColumnsForPeriod(): Int {
        return when (_selectedPeriod.value) {
            StatisticsPeriod.DAY -> 24  // 24 hours
            StatisticsPeriod.WEEK -> 7  // 7 days
            StatisticsPeriod.MONTH -> 28 // 28 days
        }
    }

    private fun buildChartData(
        focusTimes: List<FocusTime>,
        startDate: LocalDate,
        maxColumns: Int
    ): List<ChartEntry> {
        val timeZone = TimeZone.currentSystemDefault()

        return when (_selectedPeriod.value) {
            StatisticsPeriod.DAY -> {
                // Group hours into buckets based on maxColumns
                val hoursPerBucket = (24 + maxColumns - 1) / maxColumns.coerceAtLeast(1)
                val bucketCount = 24 / hoursPerBucket
                val hourData = MutableList(bucketCount) { 0 }

                focusTimes.forEach { ft ->
                    val instant = Instant.fromEpochMilliseconds(ft.finishedAt)
                    val dateTime = instant.toLocalDateTime(timeZone)
                    val bucketIndex = dateTime.hour / hoursPerBucket
                    if (bucketIndex < bucketCount) {
                        hourData[bucketIndex] += ft.duration / 60
                    }
                }

                hourData.mapIndexed { index, minutes ->
                    val startHour = index * hoursPerBucket
                    val endHour = (index + 1) * hoursPerBucket - 1
                    val label = if (hoursPerBucket == 1) {
                        "${startHour}:00"
                    } else {
                        "${startHour}-${endHour}h"
                    }
                    ChartEntry(label, minutes)
                }
            }
            StatisticsPeriod.WEEK -> {
                // Week has exactly 7 days, no aggregation needed
                val dayData = MutableList(7) { 0 }
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                focusTimes.forEach { ft ->
                    val instant = Instant.fromEpochMilliseconds(ft.finishedAt)
                    val dateTime = instant.toLocalDateTime(timeZone)
                    val dayIndex = dateTime.dayOfWeek.ordinal
                    dayData[dayIndex] += ft.duration / 60
                }
                dayData.mapIndexed { index, minutes ->
                    ChartEntry(dayNames[index], minutes)
                }
            }
            StatisticsPeriod.MONTH -> {
                // Group days into buckets based on maxColumns
                val daysInMonth = 28 // Show first 28 days
                val daysPerBucket = (daysInMonth + maxColumns - 1) / maxColumns.coerceAtLeast(1)
                val bucketCount = daysInMonth / daysPerBucket
                val dayData = MutableList(bucketCount) { 0 }

                focusTimes.forEach { ft ->
                    val instant = Instant.fromEpochMilliseconds(ft.finishedAt)
                    val dateTime = instant.toLocalDateTime(timeZone)
                    val dayOfMonth = dateTime.dayOfMonth - 1
                    if (dayOfMonth in 0 until daysInMonth) {
                        val bucketIndex = dayOfMonth / daysPerBucket
                        if (bucketIndex < bucketCount) {
                            dayData[bucketIndex] += ft.duration / 60
                        }
                    }
                }

                dayData.mapIndexed { index, minutes ->
                    val startDay = index * daysPerBucket + 1
                    val endDay = ((index + 1) * daysPerBucket).coerceAtMost(daysInMonth)
                    val label = if (daysPerBucket == 1) {
                        "$startDay"
                    } else {
                        "$startDay-$endDay"
                    }
                    ChartEntry(label, minutes)
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
