package dev.zhdanov.apps.composeApp.screens.statistics

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.zhdanov.apps.composeApp.components.pane.SectionDetailPlaceholder
import dev.zhdanov.apps.composeApp.components.pane.SectionListItem
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.testing.UiTestTags

@Composable
fun StatisticsListPane(
    selected: Screen?,
    onItemClick: (Screen) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .testTag(UiTestTags.StatisticsListPane)
    ) {
        item {
            SectionListItem(
                icon = Icons.Outlined.BarChart,
                title = Screen.StatisticsFocus.title,
                selected = selected == Screen.StatisticsFocus,
                onItemClick = { onItemClick(Screen.StatisticsFocus) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.StatisticsFocus.title))
            )
        }
        item {
            SectionListItem(
                icon = Icons.Outlined.Repeat,
                title = Screen.StatisticsRoutines.title,
                selected = selected == Screen.StatisticsRoutines,
                onItemClick = { onItemClick(Screen.StatisticsRoutines) },
                modifier = Modifier.testTag(UiTestTags.sectionItem(Screen.StatisticsRoutines.title))
            )
        }
    }
}

@Composable
fun StatisticsDetailPlaceholder() {
    SectionDetailPlaceholder(text = "Select a statistics section")
}
