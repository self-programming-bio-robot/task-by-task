import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.example.compose.AppTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import services.TodoRepository
import services.TodoService
import views.TimerButton
import views.tabs.TimerTab
import views.tabs.TodayTab
import viewsModels.SelectedTodoViewModel
import viewsModels.TimerViewModel

@Composable
fun App() {
    KoinApplication(application = {
        modules(module {
            single { TimerViewModel(todoService = get()) }
            single { TodoService(TodoRepository()) }
            single { SelectedTodoViewModel() }
        })
    }) {
        CompositionLocalProvider {}
        AppTheme {
            TabNavigator(TodayTab) {
                Scaffold(
                    content = {
                        Box(
                            Modifier
                                .padding(bottom = it.calculateBottomPadding())
                        ) {
                            CurrentTab()
                            if (LocalTabNavigator.current.current != TimerTab) {
                                TimerButton(
                                    Modifier
                                        .padding(all = 16.dp)
                                        .padding(bottom = 48.dp)
                                        .align(alignment = Alignment.BottomCenter)
                                )
                            }
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            TabNavigationItem(TodayTab)
                            TabNavigationItem(TimerTab)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TabNavigationItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) }
    )
}

expect fun getPlatformName(): String