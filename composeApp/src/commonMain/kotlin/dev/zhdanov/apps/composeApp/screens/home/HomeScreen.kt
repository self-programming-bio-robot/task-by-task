package dev.zhdanov.apps.composeApp.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import dev.zhdanov.apps.composeApp.components.timer.TimerView
import dev.zhdanov.apps.composeApp.components.topBar.TopBar
import dev.zhdanov.apps.composeApp.screens.history.AssistantReviewResponse
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFinishDay: (review: AssistantReviewResponse) -> Unit
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val windowInfo = currentWindowAdaptiveInfo()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val isActive by viewModel.isActive.collectAsState()

    val navigator = rememberSupportingPaneScaffoldNavigator<String>()
    val padding = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT) 0.dp else 16.dp
    val shape = if (windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT)
        RectangleShape else MaterialTheme.shapes.medium

    Scaffold(
        topBar = {
            TopBar(
                title = "Home",
                actions = {
                    IconButton(
                        enabled = isActive,
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                onFinishDay(viewModel.finishDay())
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                            contentDescription = "Finish day"
                        )
                    }
                }
            )
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
                                contentAlignment = Alignment.Center,
                            ) {
                                TimerView()

                                if (isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = { }
                                            )
                                            .background(Color.White.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(50.dp)
                                        )
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
