package dev.zhdanov.apps.composeApp.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.DaySummaryService
import dev.zhdanov.apps.composeApp.services.RoutineDataService
import dev.zhdanov.apps.shared.model.Routine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TaskRoutinesViewModel(
    private val routineDataService: RoutineDataService,
    daySummaryService: DaySummaryService
) : ViewModel() {
    private val _routines = MutableStateFlow<List<Routine>>(emptyList())
    val routines = _routines.asStateFlow()

    init {
        loadRoutines()
        val refreshEvents: Flow<Unit> = merge(
            daySummaryService.finishDayEvents,
            routineDataService.routineEvents
        )
        refreshEvents
            .onEach { loadRoutines() }
            .launchIn(viewModelScope)
    }

    fun loadRoutines() {
        viewModelScope.launch {
            _routines.value = routineDataService.getAllRoutines()
        }
    }

    fun addNewRoutine(title: String, description: String? = null) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isNotEmpty()) {
            viewModelScope.launch {
                routineDataService.addRoutine(trimmedTitle, description)
                loadRoutines()
            }
        }
    }

    fun updateRoutine(routine: Routine) {
        viewModelScope.launch {
            routineDataService.updateRoutine(routine)
            loadRoutines()
        }
    }

    fun toggleRoutineActive(id: Long) {
        viewModelScope.launch {
            routineDataService.toggleRoutineActive(id)
            loadRoutines()
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            routineDataService.deleteRoutine(id)
            loadRoutines()
        }
    }
}
