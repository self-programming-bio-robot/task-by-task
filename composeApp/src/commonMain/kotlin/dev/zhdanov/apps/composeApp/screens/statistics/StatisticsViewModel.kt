package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.StatisticsDataService
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
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

data class ChartEntry(
    val label: String,
    val focusTimeMinutes: Int
)

class StatisticsViewModel(
    private val statisticsDataService: StatisticsDataService
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.DAY)
    val selectedPeriod: StateFlow<StatisticsPeriod> = _selectedPeriod.asStateFlow()

    private val _periodOffset = MutableStateFlow(0)
    val periodOffset: StateFlow<Int> = _periodOffset.asStateFlow()

    private val _periodLabel = MutableStateFlow("")
    val periodLabel: StateFlow<String> = _periodLabel.asStateFlow()

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
    private var rawEndDate: LocalDate? = null

    init {
        loadData()
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        _periodOffset.value = 0
        loadData()
    }

    fun goToPreviousPeriod() {
        _periodOffset.value -= 1
        loadData()
    }

    fun goToNextPeriod() {
        _periodOffset.value += 1
        loadData()
    }

    fun goToCurrentPeriod() {
        _periodOffset.value = 0
        loadData()
    }

    /**
     * Update chart data aggregation based on available columns.
     * Call this when window size changes.
     *
     * @param maxColumns Maximum number of columns that can be displayed
     */
    fun updateColumnCount(maxColumns: Int) {
        val startDate = rawStartDate
        val endDate = rawEndDate
        if (startDate != null && endDate != null) {
            _chartData.value = buildChartData(rawFocusTimes, startDate, endDate, maxColumns)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadData() {
        viewModelScope.launch {
            val (from, to, startDate, endDate) = getDateRange()
            val focusTimes = statisticsDataService.getFocusTimesBetween(from, to)

            // Store raw data for re-aggregation
            rawFocusTimes = focusTimes
            rawStartDate = startDate
            rawEndDate = endDate

            // Focus time in seconds
            _focusTimeData.value = focusTimes.sumOf { it.duration.toLong() }

            // Work cycles count
            _workCyclesData.value = focusTimes.size

            // Tasks data - filter by selected period
            val allTasks = statisticsDataService.getAllTasks()
            val timeZone = TimeZone.currentSystemDefault()

            // Tasks created in period
            _tasksCreatedData.value = allTasks.count { task ->
                val createdEpochMs = task.createdAt.toInstant(timeZone).toEpochMilliseconds()
                createdEpochMs in from..to
            }

            // Tasks completed in period
            _tasksDoneData.value = allTasks.count { task ->
                task.completedAt?.let { completedAt ->
                    val completedEpochMs = completedAt.toInstant(timeZone).toEpochMilliseconds()
                    completedEpochMs in from..to
                } ?: false
            }

            // Chart data with default column count
            _chartData.value = buildChartData(focusTimes, startDate, endDate, getMaxColumnsForPeriod())
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
        endDate: LocalDate,
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
                val daysInMonth = endDate.dayOfMonth
                val daysPerBucket = (daysInMonth + maxColumns - 1) / maxColumns.coerceAtLeast(1)
                val bucketCount = (daysInMonth + daysPerBucket - 1) / daysPerBucket
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
    private fun getDateRange(): DateRange {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val currentDateTime = now.toLocalDateTime(timeZone)
        val today = currentDateTime.date
        val offset = _periodOffset.value

        val (baseFromDate, periodEnd) = when (_selectedPeriod.value) {
            StatisticsPeriod.DAY -> {
                val baseDate = today.plus(offset, DateTimeUnit.DAY)
                Pair(baseDate, baseDate)
            }
            StatisticsPeriod.WEEK -> {
                val weekStart = today.plus(offset * 7, DateTimeUnit.DAY)
                    .let { it.minus(it.dayOfWeek.ordinal, DateTimeUnit.DAY) }
                val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
                Pair(weekStart, weekEnd)
            }
            StatisticsPeriod.MONTH -> {
                // Calculate target month
                var targetYear = today.year
                var targetMonth = today.monthNumber + offset
                while (targetMonth > 12) {
                    targetMonth -= 12
                    targetYear++
                }
                while (targetMonth < 1) {
                    targetMonth += 12
                    targetYear--
                }
                val baseDate = LocalDate(targetYear, targetMonth, 1)
                val lastDayOfMonth = baseDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
                Pair(baseDate, lastDayOfMonth)
            }
        }

        // Update period label
        _periodLabel.value = when (_selectedPeriod.value) {
            StatisticsPeriod.DAY -> {
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${dayNames[baseFromDate.dayOfWeek.ordinal]}, ${months[baseFromDate.monthNumber - 1]} ${baseFromDate.dayOfMonth}"
            }
            StatisticsPeriod.WEEK -> {
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                if (baseFromDate.month == periodEnd.month) {
                    "${months[baseFromDate.monthNumber - 1]} ${baseFromDate.dayOfMonth} - ${periodEnd.dayOfMonth}"
                } else {
                    "${months[baseFromDate.monthNumber - 1]} ${baseFromDate.dayOfMonth} - ${months[periodEnd.monthNumber - 1]} ${periodEnd.dayOfMonth}"
                }
            }
            StatisticsPeriod.MONTH -> {
                val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                "${months[baseFromDate.monthNumber - 1]} ${baseFromDate.year}"
            }
        }

        val fromInstant = LocalDateTime(baseFromDate, LocalTime(0, 0))
            .toInstant(timeZone)

        // For past periods, end at the end of that period; for current/future, end at now
        val toEpochMs = if (periodEnd < today) {
            LocalDateTime(periodEnd, LocalTime(23, 59, 59, 999_000_000))
                .toInstant(timeZone)
                .toEpochMilliseconds()
        } else {
            now.toEpochMilliseconds()
        }

        return DateRange(
            from = fromInstant.toEpochMilliseconds(),
            to = toEpochMs,
            startDate = baseFromDate,
            endDate = periodEnd
        )
    }
}

private data class DateRange(
    val from: Long,
    val to: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
)
