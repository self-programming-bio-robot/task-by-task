package dev.zhdanov.apps.composeApp.screens.routines

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import dev.zhdanov.apps.composeApp.navigation.Screen
import dev.zhdanov.apps.composeApp.screens.statistics.StatisticsListPane
import dev.zhdanov.apps.composeApp.screens.tasks.RoutineDetails
import dev.zhdanov.apps.composeApp.screens.tasks.RoutineItem
import dev.zhdanov.apps.composeApp.screens.tasks.RoutineList
import dev.zhdanov.apps.composeApp.screens.tasks.TasksListPane
import dev.zhdanov.apps.composeApp.services.CellStatus
import dev.zhdanov.apps.composeApp.services.RoutineMatrixCell
import dev.zhdanov.apps.composeApp.services.RoutinePeriod
import dev.zhdanov.apps.composeApp.services.RoutineReportRow
import dev.zhdanov.apps.composeApp.testing.UiTestTags
import dev.zhdanov.apps.shared.model.Routine
import dev.zhdanov.apps.shared.utils.toLong
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RoutinesUiTest {
    @Test
    fun `routine item displays title description and triggers callbacks`() = runComposeUiTest {
        val routine = Routine(
            id = 1,
            title = "Morning Workout",
            description = "30 mins cardio",
            createdAt = LocalDateTime(2026, 5, 16, 8, 0)
        )
        var rowClicks = 0
        var toggleClicks = 0
        var deleteClicks = 0

        setContent {
            MaterialTheme {
                RoutineItem(
                    routine = routine,
                    onClick = { rowClicks++ },
                    onToggleActive = { toggleClicks++ },
                    onDelete = { deleteClicks++ }
                )
            }
        }

        onNodeWithText("Morning Workout").assertIsDisplayed()
        onNodeWithText("30 mins cardio").assertIsDisplayed()

        onNodeWithTag(UiTestTags.routineRow(routine.id)).performClick()
        onNodeWithTag(UiTestTags.routineActiveToggle(routine.id)).performClick()
        onNodeWithTag(UiTestTags.routineDeleteButton(routine.id)).performClick()

        assertEquals(1, rowClicks)
        assertEquals(1, toggleClicks)
        assertEquals(1, deleteClicks)
    }

    @Test
    fun `routine details edits title description and supports delete`() = runComposeUiTest {
        val routine = Routine(
            id = 5,
            title = "Evening Walk",
            description = "20 mins outside",
            createdAt = LocalDateTime(2026, 5, 16, 8, 0)
        )
        var savedRoutine: Routine? = null
        var deleted = false

        setContent {
            MaterialTheme {
                RoutineDetails(
                    routine = routine,
                    onUpdatedRoutine = { savedRoutine = it },
                    onDeleteRoutine = { deleted = true },
                    onCancel = {}
                )
            }
        }

        onNodeWithText("Evening Walk").assertIsDisplayed()
        onNodeWithText("20 mins outside").assertIsDisplayed()
        onNodeWithText("Save").performClick()
        onNodeWithTag(UiTestTags.RoutineDeleteInEditorButton).performClick()

        assertEquals(routine, savedRoutine)
        assertEquals(true, deleted)
    }

    @Test
    fun `routine list submits trimmed routine and shows empty placeholder`() = runComposeUiTest {
        val added = mutableListOf<String>()

        setContent {
            MaterialTheme {
                RoutineList(
                    routines = emptyList(),
                    onAddRoutine = added::add,
                    onToggleActive = {},
                    onDelete = {}
                )
            }
        }

        onNodeWithText("No routines yet").assertIsDisplayed()
        onNodeWithTag(UiTestTags.NewRoutineInput).performTextInput("  Read 20 pages  ")
        onNodeWithTag(UiTestTags.AddRoutineButton).performClick()

        assertEquals(listOf("Read 20 pages"), added)
    }

    @Test
    fun `tasks and statistics list panes select sections like settings`() = runComposeUiTest {
        val selectedTasks = mutableListOf<Screen>()
        val selectedStats = mutableListOf<Screen>()

        setContent {
            MaterialTheme {
                Column {
                    TasksListPane(
                        selected = Screen.TaskList(),
                        onItemClick = selectedTasks::add
                    )
                    StatisticsListPane(
                        selected = Screen.StatisticsFocus,
                        onItemClick = selectedStats::add
                    )
                }
            }
        }

        onNodeWithTag(UiTestTags.sectionItem("All Tasks")).performClick()
        onNodeWithTag(UiTestTags.sectionItem("Focus Statistics")).performClick()

        assertEquals(listOf<Screen>(Screen.TaskList()), selectedTasks)
        assertEquals(listOf<Screen>(Screen.StatisticsFocus), selectedStats)
    }

    @Test
    fun `period selector and matrix grid render cells and streak`() = runComposeUiTest {
        val selectedPeriods = mutableListOf<RoutinePeriod>()
        val date1 = LocalDate(2026, 5, 15)
        val date2 = LocalDate(2026, 5, 16)
        val routine = Routine(
            id = 42,
            title = "Meditation",
            createdAt = LocalDateTime(2026, 5, 1, 8, 0)
        )
        val row = RoutineReportRow(
            routine = routine,
            cells = listOf(
                RoutineMatrixCell(date = date1, status = CellStatus.COMPLETED, taskId = 10L),
                RoutineMatrixCell(date = date2, status = CellStatus.PENDING, taskId = 11L)
            ),
            completionRate = 0.5f,
            currentStreak = 3
        )

        setContent {
            MaterialTheme {
                Column {
                    PeriodSelector(
                        selectedPeriod = RoutinePeriod.WEEK,
                        onSelectPeriod = selectedPeriods::add
                    )
                    RoutineMatrixGrid(reportRows = listOf(row))
                }
            }
        }

        onNodeWithText("Meditation").assertIsDisplayed()
        onNodeWithText("3 day streak").assertIsDisplayed()
        onNodeWithTag(UiTestTags.routineMatrixCell(42, date1.toLong())).assertIsDisplayed()
        onNodeWithTag(UiTestTags.routineMatrixCell(42, date2.toLong())).assertIsDisplayed()

        onNodeWithTag(UiTestTags.routinePeriodChip(RoutinePeriod.TWO_WEEKS.name)).performClick()
        onNodeWithTag(UiTestTags.routinePeriodChip(RoutinePeriod.MONTH.name)).performClick()

        assertEquals(listOf(RoutinePeriod.TWO_WEEKS, RoutinePeriod.MONTH), selectedPeriods)
    }
}
