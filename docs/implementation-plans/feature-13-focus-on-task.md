# Implementation Plan: Feature 13 - Focus on Task (Remaining Subtasks)

## Overview
Complete the remaining subtasks of Feature 13 from README.md:
- 13.2 ✅ Link selected task to focus time entity (DONE)
- 13.3 Update statistics logic to support focus-task linkage
- 13.4 UI indication of focused task during focus session
- 13.5 Display linked focus task in day statistics

## Current State

### Already Completed (by architect-2)
- Database schema updated with `taskId` column
- Data models updated (FocusTime.taskId, CreateFocusTime.taskId)
- Database operations updated to handle taskId
- FocusTaskService created for state management
- TimerViewModel updated to save focusedTaskId
- TaskListScreen updated with focus button
- DI configured in AppModule

### Remaining Work
- **13.4**: Display focused task in Timer UI (currently shows task list focus button)
- **13.5**: Update History/Statistics to show linked tasks

## Implementation Plan

### Phase 1: Display Focused Task in Timer UI

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerView.kt`

#### 1.1 Add FocusedTaskDisplay composable
```kotlin
@Composable
private fun FocusedTaskDisplay(task: Task) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Focused task",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Clear button
            IconButton(
                onClick = { /* TODO: Implement clearing */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear focused task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
```

#### 1.2 Update TimerView to use FocusTaskService
```kotlin
@OptIn(KoinExperimentalAPI::class, ExperimentalTime::class)
@Composable
@Preview
fun TimerView() {
    val viewModel = koinInject<TimerViewModel>()
    val focusTaskService = koinInject<FocusTaskService>()
    // ... existing state collection
    val focusedTask by focusTaskService.focusedTask.collectAsState()

    Column {
        // Show focused task if present
        focusedTask?.let { task ->
            FocusedTaskDisplay(task = task)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Existing timer components...
    }
}
```

**Required imports**:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Surface
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import dev.zhdanov.apps.shared.model.Task
```

#### 1.3 Add clear button functionality
Update FocusTaskService with clear button:
```kotlin
class FocusTaskService() {
    private val _focusedTask = MutableStateFlow<Task?>(null)
    val focusedTask: StateFlow<Task?> = _focusedTask.asStateFlow()

    fun setFocusedTask(task: Task?) {
        _focusedTask.value = task
    }

    fun clearFocusedTask() {
        _focusedTask.value = null
    }

    fun getFocusedTaskId(): Long? = _focusedTask.value?.id
}
```

### Phase 2: Update History/Statistics to Show Linked Tasks

**Files**:
- `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt`
- `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt`

#### 2.1 Create FocusTimeWithTask model
**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/HistoryModels.kt` (NEW)

```kotlin
@Serializable
data class FocusTimeWithTask(
    val focusTime: FocusTime,
    val task: Task?
)
```

#### 2.2 Update HistoryViewModel to load tasks
```kotlin
class HistoryViewModel(
    private val database: Database,
    private val taskRepository: TaskRepository, // NEW
) : ViewModel() {
    private val _focusTimes = MutableStateFlow<List<FocusTimeWithTask>>(emptyList())

    val focusTimes = _focusTimes.asStateFlow()

    init {
        loadFocusHistory()
    }

    private fun loadFocusHistory() {
        viewModelScope.launch {
            val focusTimes = database.getAllFocusTimes()
            val tasks = getAllTasks() // Cache tasks

            _focusTimes.value = focusTimes.map { focusTime ->
                FocusTimeWithTask(
                    focusTime = focusTime,
                    task = focusTime.taskId?.let { taskId ->
                        tasks.find { it.id == taskId }
                    }
                )
            }
        }
    }

    private fun getAllTasks(): List<Task> {
        // Implement task caching or load from database
        return emptyList() // Placeholder
    }
}
```

#### 2.3 Update HistoryScreen UI
```kotlin
@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = koinViewModel<HistoryViewModel>()
    val focusTimes by viewModel.focusTimes.collectAsState()

    LazyColumn {
        items(focusTimes) { item ->
            FocusTimeWithTaskItem(item)
        }
    }
}

@Composable
private fun FocusTimeWithTaskItem(item: FocusTimeWithTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Duration display
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.focusTime.duration / 60} min",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.focusTime.feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Task display
            item.task?.let { task ->
                Spacer(modifier = Modifier.width(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
```

#### 2.4 Update HistoryViewModel constructor
Add `taskRepository` parameter to HistoryViewModel:
```kotlin
// In HistoryScreen.kt
val viewModel: HistoryViewModel = koinViewModel<HistoryViewModel>()

// In AppModule.kt
viewModel { HistoryViewModel(get(), get()) } // Add taskRepository parameter
```

### Phase 3: Statistics Enhancement (Optional - Future Enhancement)

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/StatisticsScreen.kt` (if exists)

#### 3.1 Add task grouping to statistics
Display breakdown of focus time by task:
- Total focus time per task
- Number of focus sessions per task
- Average session duration per task

## Implementation Order

1. **Update FocusTaskService** - Add clear button functionality (5 min)
2. **Update TimerView** - Add FocusedTaskDisplay composable (10 min)
3. **Create model** - Add FocusTimeWithTask data class (5 min)
4. **Update HistoryViewModel** - Add task repository, load tasks, map to FocusTimeWithTask (15 min)
5. **Update HistoryScreen** - Display linked tasks in history list (20 min)
6. **Test** - Verify focus task links display correctly (10 min)

**Total estimated time**: ~65 minutes

## Testing Checklist

- [ ] Focus button in task list toggles focus state
- [ ] Focused task displays in Timer UI
- [ ] Clear button removes focused task
- [ ] Focus session saves with correct taskId
- [ ] History shows linked tasks
- [ ] Clicking task in history navigates to task details
- [ ] Statistics aggregate by task correctly
- [ ] No memory leaks from task caching

## Files to Modify

1. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` - Add clear method
2. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerView.kt` - Add FocusedTaskDisplay
3. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/HistoryModels.kt` - NEW FILE
4. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt` - Load tasks, map to FocusTimeWithTask
5. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt` - Display linked tasks
6. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt` - Add taskRepository to HistoryViewModel

## Notes

- Task retrieval should be optimized (cache in ViewModel)
- Task titles may be long - truncate appropriately in UI
- Consider adding task click navigation from history
- Statistics aggregation is a future enhancement (separate task)
