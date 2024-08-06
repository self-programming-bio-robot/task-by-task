package dev.zhdanov.apps.composeApp.screens.home

import androidx.lifecycle.ViewModel

class HomeViewModel: ViewModel() {
    init {
        println("Home ViewModel initializing...")
    }

    override fun onCleared() {
        super.onCleared()
        println("Home ViewModel clearing...")
    }
}