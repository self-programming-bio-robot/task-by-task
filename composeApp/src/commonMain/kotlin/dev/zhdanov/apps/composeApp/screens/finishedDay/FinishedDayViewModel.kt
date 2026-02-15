package dev.zhdanov.apps.composeApp.screens.finishedDay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zhdanov.apps.composeApp.services.ChatService
import dev.zhdanov.apps.shared.model.ChatSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class FinishedDayViewModel(
    private val chatService: ChatService
) : ViewModel() {

    val currentSession: StateFlow<ChatSession?> = chatService.currentSession
    val isLoading: StateFlow<Boolean> = chatService.isLoading
    val error: StateFlow<String?> = chatService.error

    fun startSession(date: LocalDate, daySummary: String) {
        chatService.startSession(date, daySummary)
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            chatService.sendMessage(message)
        }
    }

    fun clearSession() {
        chatService.clearSession()
    }
}
