package dev.zhdanov.apps.composeApp.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey

class NavigationViewModel : ViewModel() {
    val backStack = mutableStateListOf<NavKey>(Screen.Home)

    fun navigateTo(key: NavKey) {
        backStack.add(key)
    }

    fun goBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    fun replaceRoot(key: NavKey) {
        backStack.clear()
        backStack.add(key)
    }

    fun navigateAndClear(key: NavKey) {
        backStack.clear()
        backStack.add(key)
    }

    fun popUpTo(key: NavKey, inclusive: Boolean = false) {
        val index = backStack.indexOf(key)
        if (index >= 0) {
            val removeCount = if (inclusive) {
                backStack.size - index
            } else {
                backStack.size - index - 1
            }
            repeat(removeCount) {
                backStack.removeLastOrNull()
            }
        }
    }
}
