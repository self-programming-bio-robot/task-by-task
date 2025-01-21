package dev.zhdanov.apps.composeApp.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.components.history.HistoryList
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = koinViewModel<HistoryViewModel>()

    val history = viewModel.history.collectAsState(listOf())
    val isLoading = viewModel.isLoading.collectAsState(initial = false)
    val windowInfo = currentWindowAdaptiveInfo()
    val coroutineScope = rememberCoroutineScope()
    val navigator = rememberSupportingPaneScaffoldNavigator<String>()
    val padding = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) 0.dp else 16.dp
    val shape = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT)
        RectangleShape else MaterialTheme.shapes.medium

    Scaffold(
        topBar = {
            TopBar("History")
        },
        content = { paddings ->
            Box(modifier = Modifier.padding(paddings)) {
                SupportingPaneScaffold(
                    modifier = Modifier
                        .padding(start = padding, end = padding, bottom = padding),
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    mainPane = {
                        AnimatedPane {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = shape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading.value) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(50.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        HistoryList(history.value)
                                    }
                                }
                            }
                        }
                    },
                    supportingPane = {
                        AnimatedPane {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = shape
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                            }
                        }
                    }
                )
            }
        }
    )
}
