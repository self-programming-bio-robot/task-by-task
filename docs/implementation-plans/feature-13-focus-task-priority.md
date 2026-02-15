# Implementation Plan: Feature 13 - Focus on Task (Priority Implementation)

## Overview
Implement remaining subtasks for Feature 13 (Focus on Task):
- 13.1 ✅ Button to select focus task (ALREADY DONE)
- 13.2 Link selected task to focus time entity
- 13.3 Update statistics logic to support focus-task linkage
- 13.4 UI indication of focused task during focus session
- 13.5 Display linked focus task in day statistics

## Priority Order
1. **13.2**: Link selected task to focus time entity (CORE DATA LAYER)
2. **13.4**: UI indication of focused task during focus session (VISUAL FEEDBACK)
3. **13.5**: Display linked focus task in day statistics (HISTORY VIEW)
4. **13.3**: Update statistics logic (AGGREGATION - can be done in parallel)

---

## Subtask 13.2: Link Selected Task to Focus Time Entity

### Database Schema Changes

**File**: `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq`

Update FocusTime table to include taskId column:
```sql
CREATE TABLE IF NOT EXISTS FocusTime (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    duration INTEGER NOT NULL,
    feedback TEXT,
    finishedAt INTEGER NOT NULL,
    startedAt INTEGER,
    pauseTime INTEGER,
    taskId INTEGER  -- NEW COLUMN
);

insertFocusTime:
INSERT INTO FocusTime (duration, feedback, finishedAt, startedAt, pauseTime, taskId)
VALUES (?, ?, ?, ?, ?, ?);  -- UPDATED
```

### Data Model Updates

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt`

Add taskId to data classes:
```kotlin
@Serializable
data class FocusTime(
    val id: Long,
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
    val startedAt: Long? = null,
    val pauseTime: Int? = null,
    val taskId: Long? = null  // NEW: Link to Task
)

@Serializable
data class CreateFocusTime(
    val duration: Int,
    val feedback: String,
    val finishedAt: Long,
    val startedAt: Long? = null,
    val pauseTime: Int? = null,
    val taskId: Long? = null  // NEW: Link to Task
)
```

### Database Layer Updates

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/Database.kt`

Update addFocusTime methods:
```kotlin
fun addFocusTime(focusTime: CreateFocusTime) {
    addFocusTime(
        duration = focusTime.duration.toLong(),
        finishedAt = focusTime.finishedAt,
        feedback = focusTime.feedback,
        startedAt = focusTime.startedAt,
        pauseTime = focusTime.pauseTime?.toLong(),
        taskId = focusTime.taskId  // NEW PARAMETER
    )
}

fun addFocusTime(
    duration: Long,
    finishedAt: Long,
    feedback: String?,
    startedAt: Long? = null,
    pauseTime: Long? = null,
    taskId: Long? = null  // NEW PARAMETER
) {
    dbQuery.transaction {
        dbQuery.insertFocusTime(
            duration = duration,
            feedback = feedback,
            finishedAt = finishedAt,
            startedAt = startedAt,
            pauseTime = pauseTime,
            taskId = taskId  // NEW COLUMN
        )
    }
}
```

### Mapper Updates

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/mappers.kt`

Update focusTimeMapper to include taskId:
```kotlin
val focusTimeMapper = {
    id: Long,
    duration: Long,
    feedback: String?,
    finishedAt: Long,
    startedAt: Long?,
    pauseTime: Long?,
    taskId: Long? ->  // NEW PARAMETER
    FocusTime(
        id = id,
        duration = duration.toInt(),
        feedback = feedback ?: "",
        finishedAt = finishedAt,
        startedAt = startedAt,
        pauseTime = pauseTime?.toInt(),
        taskId = taskId  // NEW FIELD
    )
}
```

---

## Subtask 13.4: UI Indication of Focused Task During Focus Session

### Focus Task Service

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` (NEW)

Create service to manage focused task state:
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

### Dependency Injection

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt`

Register FocusTaskService:
```kotlin
val appModule = module {
    single { NotificationService() }
    single { StartOfDayService(get()) }
    single { DaySummaryService(get(), get(), get()) }
    single { TimerSettingsService(get()) }
    single { FocusTaskService() }  // NEW
    single { TimerViewModel(get(), get(), get()) }  // UPDATE: Add FocusTaskService
    single { GeneralSettingsViewModel(get()) }

    viewModel { HomeViewModel(get(), get(), get()) }
    // ... other ViewModels
}
```

### Timer ViewModel Integration

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerViewModel.kt`

Update to inject FocusTaskService and save taskId:
```kotlin
class TimerViewModel(
    private val notificationService: NotificationService,
    private val database: Database,
    private val timerSettingsService: TimerSettingsService,
    private val focusTaskService: FocusTaskService,  // NEW
) : ViewModel() {
    // ... existing properties

    fun saveFeedback(feedback: CreateFocusTime) {
        viewModelScope.launch {
            val pauseTime = calculatePauseTime()
            val focusedTaskId = focusTaskService.getFocusedTaskId()  // NEW

            val focusTimeWithPause = CreateFocusTime(
                duration = feedback.duration,
                feedback = feedback.feedback,
                finishedAt = feedback.finishedAt,
                startedAt = _focusSessionStart.value,
                pauseTime = pauseTime,
                taskId = focusedTaskId  // NEW: Link to task
            )

            database.addFocusTime(focusTimeWithPause)

            // Optionally clear focused task after saving
            if (focusedTaskId != null) {
                focusTaskService.clearFocusedTask()
            }

            resetFocusTracking()
        }
    }
}
```

### Task List UI - Focus Button

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt`

Add focus button to task items:
```kotlin
@Composable
fun TaskList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit
) {
    val viewModel: TaskListViewModel = koinViewModel<TaskListViewModel>()
    val focusTaskService: FocusTaskService = koinInject()  // NEW
    val focusedTask by focusTaskService.focusedTask.collectAsState()  // NEW

    Column {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                NewTaskInput(
                    onAddTask = viewModel::addNewTask
                )
            }
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    focusedTask = focusedTask,  // NEW
                    onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                    onAddToday = { viewModel.updateTask(task.copy(isToday = it)) },
                    onClick = { onTaskClick(task) },
                    onFocus = { focusTaskService.setFocusedTask(it) }  // NEW
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    focusedTask: Task?,  // NEW
    onToggleCompletion: () -> Unit,
    onAddToday: (Boolean) -> Unit,
    onClick: () -> Unit,
    onFocus: (Task) -> Unit  // NEW
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion() }
        )

        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))

        // NEW: Focus button with visual feedback
        IconButton(onClick = { onFocus(task) }) {
            Icon(
                imageVector = if (focusedTask?.id == task.id) {
                    Icons.Default.CenterFocusStrong
                } else {
                    Icons.Default.CenterFocusWeak
                },
                contentDescription = "Focus on this task",
                tint = if (focusedTask?.id == task.id) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        IconToggleButton(
            checked = task.isToday,
            onCheckedChange = { onAddToday(it) },
        ) {
            Icon(
                imageVector = Icons.Default.Today,
                contentDescription = "Today",
            )
        }
    }
}
```

### Required Imports

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt`

Add missing imports:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import org.koin.compose.koinInject
```

---

## Subtask 13.5: Display Linked Focus Task in Day Statistics

### History View Model Enhancement

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/HistoryModels.kt` (NEW)

Create model to combine FocusTime with Task:
```kotlin
@Serializable
data class FocusTimeWithTask(
    val focusTime: FocusTime,
    val task: Task?
)
```

### History View Model Updates

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt`

Update to load tasks and map to FocusTimeWithTask:
```kotlin
class HistoryViewModel(
    private val database: Database,
    private val taskRepository: TaskRepository  // NEW
) : ViewModel() {
    private val _focusTimes = MutableStateFlow<List<FocusTimeWithTask>>(emptyList())
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())  // NEW

    val focusTimes = _focusTimes.asStateFlow()

    init {
        loadFocusHistory()
        loadTasks()  // NEW
    }

    private fun loadFocusHistory() {
        viewModelScope.launch {
            val focusTimes = database.getAllFocusTimes()
            _focusTimes.value = focusTimes.map { focusTime ->
                FocusTimeWithTask(
                    focusTime = focusTime,
                    task = focusTime.taskId?.let { taskId ->
                        _tasks.value.find { it.id == taskId }
                    }
                )
            }
        }
    }

    private fun loadTasks() {  // NEW
        viewModelScope.launch {
            _tasks.value = taskRepository.getAllTasks()
        }
    }
}
```

### History Screen UI Updates

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt`

Update to display linked tasks:
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

            // NEW: Task display
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

### Required Imports

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt`

Add missing imports:
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Surface
import dev.zhdanov.apps.shared.model.Task  // For FocusTimeWithTask
```

### DI Updates

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt`

Update HistoryViewModel to include taskRepository:
```kotlin
viewModel { HistoryViewModel(get(), get()) }  // Add taskRepository parameter
```

---

## Subtask 13.3: Update Statistics Logic (Optional - Can Be Done Later)

### Statistics Aggregation (Future Enhancement)

This can be implemented later as a separate enhancement:
- Group focus time by task
- Show total focus time per task
- Show number of sessions per task
- Average session duration per task

---

## Implementation Order

### Phase 1: Core Data Layer (13.2) - CRITICAL PATH
1. Update `AppDatabase.sq` - Add taskId column to FocusTime table (5 min)
2. Update `focus.kt` - Add taskId to data models (5 min)
3. Update `mappers.kt` - Add taskId to focusTimeMapper (5 min)
4. Update `Database.kt` - Add taskId parameter to addFocusTime (10 min)
5. **TEST COMPILE** - Verify data layer compiles (5 min)

### Phase 2: Service & Integration (13.4) - VISUAL FEEDBACK
6. Create `FocusTaskService.kt` (10 min)
7. Update `AppModule.kt` - Register FocusTaskService (5 min)
8. Update `TimerViewModel.kt` - Inject FocusTaskService, save taskId (10 min)
9. Update `TaskListScreen.kt` - Add focus button with icons (15 min)
10. **TEST** - Run app and verify focus button works (10 min)

### Phase 3: History View (13.5) - STATISTICS DISPLAY
11. Create `HistoryModels.kt` - Add FocusTimeWithTask (5 min)
12. Update `HistoryViewModel.kt` - Load tasks, map to FocusTimeWithTask (15 min)
13. Update `HistoryScreen.kt` - Display linked tasks (20 min)
14. **TEST** - Verify history shows tasks correctly (10 min)

---

## Testing Checklist

### Data Layer (13.2)
- [ ] Database schema compiles with taskId column
- [ ] FocusTime data model includes taskId field
- [ ] CreateFocusTime data model includes taskId field
- [ ] Mapper includes taskId parameter
- [ ] Database.addFocusTime accepts taskId parameter
- [ ] Code compiles without errors

### Service Layer (13.4)
- [ ] FocusTaskService created and registered in DI
- [ ] TimerViewModel injects FocusTaskService
- [ ] FocusTaskId retrieved and saved in addFocusTime
- [ ] Focused task cleared after saving (if set)

### UI Layer (13.4)
- [ ] Focus button appears in task list
- [ ] Focus button highlights when task is focused (CenterFocusStrong icon)
- [ ] Focus button uses correct color (primary vs onSurfaceVariant)
- [ ] Clicking focus button updates FocusTaskService state

### History View (13.5)
- [ ] FocusTimeWithTask model created
- [ ] HistoryViewModel loads tasks
- [ ] HistoryViewModel maps FocusTime to FocusTimeWithTask
- [ ] HistoryScreen displays linked tasks
- [ ] Task display shows icon and title
- [ ] Tasks loaded correctly when focus has taskId

---

## Risk Mitigation

### Database Migration
- **Risk**: Adding column breaks existing queries
- **Mitigation**: All queries updated to include taskId parameter
- **Fallback**: taskId is nullable, maintains backward compatibility

### Task Loading
- **Risk**: Loading all tasks on every history load
- **Mitigation**: Cache tasks in ViewModel, load once
- **Performance**: Use lazy loading or pagination for large datasets

### Null Safety
- **Risk**: Task may be deleted but FocusTime references it
- **Mitigation**: Task is optional (?), UI shows "Unknown task" or similar
- **Display**: Gracefully handle null task in UI

---

## Files to Modify

### Phase 1: Data Layer (13.2)
1. `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq`
2. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt`
3. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/mappers.kt`
4. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/Database.kt`

### Phase 2: Service & Integration (13.4)
5. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` (NEW)
6. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt`
7. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerViewModel.kt`
8. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt`

### Phase 3: History View (13.5)
9. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/HistoryModels.kt` (NEW)
10. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt`
11. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt`
12. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt`

---

## Notes

- **Backward Compatibility**: taskId is nullable, existing data without tasks still works
- **Data Integrity**: FocusTime.taskId references Task.id, no foreign key constraint
- **Performance**: Task list caching recommended for large task lists
- **UX**: Clear focused task after saving focus session provides feedback loop
- **Migration**: No ALTER TABLE support on all platforms, manual table recreation required

**Total estimated time**: ~90 minutes
