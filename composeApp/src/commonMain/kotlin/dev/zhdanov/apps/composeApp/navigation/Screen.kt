package dev.zhdanov.apps.composeApp.navigation

sealed class  Screen(val route: String) {
    object Home : Screen("home")
}