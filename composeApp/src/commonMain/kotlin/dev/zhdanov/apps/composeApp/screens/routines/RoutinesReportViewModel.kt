package dev.zhdanov.apps.composeApp.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.LogicalDayService
import dev.zhdanov.apps.composeApp.services.RoutineDataService
import dev.zhdanov.apps.composeApp.services.RoutinePeriod
import dev.zhdanov.apps.composeApp.services.RoutineReportRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

class RoutinesReportViewModel(
    private val routineDataService: RoutineDataService,
    private val logicalDayService: LogicalDayService,
    daySummaryService: DaySummaryService
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(RoutinePeriod.WEEK)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _periodOffset = MutableStateFlow(0)
    val periodOffset = _periodOffset.asStateFlow()

    private val _periodLabel = MutableStateFlow("")
    val periodLabel = _periodLabel.asStateFlow()

    private val _reportRows = MutableStateFlow<List<RoutineReportRow>>(emptyList())
    val reportRows = _reportRows.asStateFlow()

    init {
        loadReport()
        val refreshEvents: Flow<Unit> = merge(
            routineDataService.routineEvents,
            daySummaryService.finishDayEvents
        )
        refreshEvents
            .onEach { loadReport() }
            .launchIn(viewModelScope)
    }

    fun setPeriod(period: RoutinePeriod) {
        _selectedPeriod.value = period
        _periodOffset.value = 0
        loadReport()
    }

    fun goToPreviousPeriod() {
        _periodOffset.value -= 1
        loadReport()
    }

    fun goToNextPeriod() {
        _periodOffset.value += 1
        loadReport()
    }

    fun goToCurrentPeriod() {
        _periodOffset.value = 0
        loadReport()
    }

    fun loadReport() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val period = _selectedPeriod.value
                val offset = _periodOffset.value
                val today = logicalDayService.currentLogicalDay()
                val endDate = today.plus(offset * period.days, DateTimeUnit.DAY)
                val startDate = endDate.minus(period.days - 1, DateTimeUnit.DAY)
                _periodLabel.value = formatDateRange(startDate, endDate)
                _reportRows.value = routineDataService.getRoutineMatrixReport(
                    period = period,
                    currentDate = today,
                    periodOffset = offset
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatDateRange(start: LocalDate, end: LocalDate): String {
        val startMonth = start.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val endMonth = end.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return if (start.year == end.year && start.month == end.month) {
            "$startMonth ${start.day} - ${end.day}, ${start.year}"
        } else if (start.year == end.year) {
            "$startMonth ${start.day} - $endMonth ${end.day}, ${end.year}"
        } else {
            "$startMonth ${start.day}, ${start.year} - $endMonth ${end.day}, ${end.year}"
        }
    }
}
