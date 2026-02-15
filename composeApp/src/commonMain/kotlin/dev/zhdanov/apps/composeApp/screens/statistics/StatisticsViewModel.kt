package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.shared.cache.Database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

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

    init {
        loadData()
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val (from, to) = getDateRange()
            val focusTimes = database.getAllFocusTimesBetween(from, to)

            // Focus time in seconds
            _focusTimeData.value = focusTimes.sumOf { it.duration.toLong() }

            // Work cycles count
            _workCyclesData.value = focusTimes.size

            // Tasks data
            val allTasks = database.taskRepository.getAllTasks()
            _tasksCreatedData.value = allTasks.size
            _tasksDoneData.value = allTasks.count { it.isCompleted }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getDateRange(): Pair<Long, Long> {
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

        return fromInstant.toEpochMilliseconds() to now.toEpochMilliseconds()
    }
}
