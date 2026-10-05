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

    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeLastOrNull()
        }
    }

    /**
     * Selects a settings section (detail pane), replacing any currently open
     * section or extra pane instead of accumulating them on the back stack.
     */
    fun selectSettingsSection(section: Screen) {
        popUntil { it == Screen.Settings }
        if (backStack.lastOrNull() != section) {
            backStack.add(section)
        }
    }

    /**
     * Opens the timer editor in the extra pane. Replaces the current extra
     * pane if one is already open.
     */
    fun openTimerEditor(timerId: Long?) {
        if (backStack.lastOrNull() is Screen.SettingsTimerEdit) {
            backStack.removeLastOrNull()
        }
        backStack.add(Screen.SettingsTimerEdit(timerId))
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
