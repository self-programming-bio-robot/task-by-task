# Implementation Plan: Feature 13 - Focus on Task (URGENT)

## Current State Analysis

### Data Models (EXAMINED)
- `FocusTime.kt` - Has `id`, `duration`, `feedback`, `finishedAt`, `startedAt`, `pauseTime`
- `CreateFocusTime.kt` - Corresponds to above
- `Task.kt` - Has `id`, `title`, `description`, `createdAt`, `completedAt`, `isCompleted`, `isToday`
- **MISSING**: No `taskId` field in FocusTime/CreateFocusTime
- **MISSING**: No foreign key relationship between FocusTime and Task

### Database Schema (EXAMINED)
- `FocusTime` table: `id`, `duration`, `feedback`, `finishedAt`, `startedAt`, `pauseTime`
- **MISSING**: No `taskId` column in FocusTime table
- **MISSING**: No foreign key constraint

### Task List UI (EXAMINED)
- `TaskListScreen.kt` (lines 140-186): Has focus button with Icons.Default.CenterFocusStrong/CenterFocusWeak
- Uses `focusTaskService` for state management
- **ALREADY IMPLEMENTED**: Focus button exists, visual feedback for focused task

### Missing Components
- No `FocusTaskService` exists
- TaskRepository exists but no TaskRepository
- History screen exists but doesn't show linked tasks

---

## Step-by-Step Implementation Plan

### Step 1: Database Schema Migration (CRITICAL - DATA LOSS RISK)

**File**: `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq`

**Action**: Add `taskId INTEGER` column to FocusTime table
**Time**: 15 minutes

```sql
-- Add taskId column to FocusTime table
ALTER TABLE FocusTime ADD COLUMN taskId INTEGER;
```

**IMPORTANT**:
- SQLDelight supports ALTER TABLE on most platforms
- This is safe for new installations
- For existing databases, column will be added with null values
- No data loss risk with ALTER TABLE (nullable column)
- Run-time migration not needed (column is nullable)

**Verification**:
```sql
-- Verify column exists (optional, for safety)
selectFocusTimesWithTask:
SELECT * FROM FocusTime;
```

---

### Step 2: Update Data Models

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt`
**Time**: 10 minutes

**Action**: Add `taskId: Long? = null` field to both FocusTime and CreateFocusTime

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

**Rationale**: Nullable to maintain backward compatibility. Focus sessions can exist without tasks.

---

### Step 3: Update Database Layer

**File**: `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq`
**Time**: 5 minutes

**Action**: Update `insertFocusTime` to include taskId parameter

```sql
insertFocusTime:
INSERT INTO FocusTime (duration, feedback, finishedAt, startedAt, pauseTime, taskId)
VALUES (?, ?, ?, ?, ?, ?, ?);  -- NEW: taskId parameter added
```

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/Database.kt`
**Time**: 10 minutes

**Action**: Update `addFocusTime` methods to pass taskId

```kotlin
// Existing overload with CreateFocusTime
fun addFocusTime(focusTime: CreateFocusTime) {
    addFocusTime(
        duration = focusTime.duration.toLong(),
        finishedAt = focusTime.finishedAt,
        feedback = focusTime.feedback,
        startedAt = focusTime.startedAt,
        pauseTime = focusTime.pauseTime?.toLong(),
        taskId = focusTime.taskId  // NEW: Pass taskId to database layer
    )
}

// Existing overload with individual parameters
fun addFocusTime(
    duration: Long,
    finishedAt: Long,
    feedback: String?,
    startedAt: Long? = null,
    pauseTime: Long? = null,
    taskId: Long? = null  // NEW: Accept taskId parameter
)
```

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/mappers.kt`
**Time**: 5 minutes

**Action**: Update `focusTimeMapper` to include taskId parameter

```kotlin
val focusTimeMapper = { id: Long, duration: Long, feedback: String?, finishedAt: Long, startedAt: Long?, pauseTime: Long?, taskId: Long? ->
    FocusTime(
        id = id,
        duration = duration.toInt(),
        feedback = feedback ?: "",
        finishedAt = finishedAt,
        startedAt = startedAt,
        pauseTime = pauseTime?.toInt(),
        taskId = taskId  // NEW: Include taskId in FocusTime object
    )
}
```

---

### Step 4: Create FocusTaskService

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` (NEW FILE)
**Time**: 15 minutes

**Action**: Create service to manage focused task state

```kotlin
package dev.zhdanov.apps.composeApp.services

import dev.zhdanov.apps.shared.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

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

**Rationale**:
- Centralized state management for focused task
- StateFlow for reactive UI updates
- Used by TaskList (focus button) and TimerViewModel (saving)

---

### Step 5: Update Dependency Injection

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt`
**Time**: 5 minutes

**Action**: Register FocusTaskService and inject into TimerViewModel

```kotlin
val appModule = module {
    single { NotificationService() }
    single { StartOfDayService(get()) }
    single { DaySummaryService(get(), get(), get()) }
    single { TimerSettingsService(get()) }
    single { FocusTaskService() }  // NEW
    single { TimerViewModel(get(), get(), get(), get()) }  // Add FocusTaskService parameter
    single { GeneralSettingsViewModel(get()) }

    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }  // Add TaskRepository if needed
    viewModel { TimersSettingsViewModel(get()) }
    viewModel { EditableTimerSettingsViewModel(get()) }
    viewModel { TaskListViewModel(get(), get()) }  // Add FocusTaskService parameter
}
```

---

### Step 6: Update TimerViewModel

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerViewModel.kt`
**Time**: 10 minutes

**Action**: Inject FocusTaskService, save taskId when finishing focus session

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
            val focusedTaskId = focusTaskService.getFocusedTaskId()  // NEW: Get focused task ID

            val focusTimeWithPause = CreateFocusTime(
                duration = feedback.duration,
                feedback = feedback.feedback,
                finishedAt = feedback.finishedAt,
                startedAt = _focusSessionStart.value,
                pauseTime = pauseTime,
                taskId = focusedTaskId  // NEW: Link to task
            )

            database.addFocusTime(focusTimeWithPause)
            resetFocusTracking()
        }
    }
}
```

---

### Step 7: Update TaskListScreen UI

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt`
**Time**: 15 minutes

**Action**: Inject FocusTaskService, show visual feedback for focused task

#### 7.1 Add FocusTaskService import

```kotlin
import dev.zhdanov.apps.composeApp.services.FocusTaskService
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
```

#### 7.2 Update TaskList composable

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
                    onFocus = { focusTaskService.setFocusedTask(it) }  // NEW: Focus button
                )
            }
        }
    }
}
```

#### 7.3 Update TaskItem composable

```kotlin
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

#### 7.4 Add missing imports

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Today
```

---

### Step 8: Update HistoryViewModel (13.3 - Statistics)

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt`
**Time**: 20 minutes

**Action**: Load tasks when building history, map to FocusTimeWithTask

```kotlin
@OptIn(ExperimentalTime::class)
class HistoryViewModel(
    private val database: Database,
    private val taskRepository: TaskRepository,  // NEW: Need to add this
) : ViewModel() {
    private val _focusTimes = MutableStateFlow<List<FocusTime>>(emptyList())

    val focusTimes = _focusTimes.asStateFlow()

    init {
        loadFocusHistory()
    }

    private fun loadFocusHistory() {
        viewModelScope.launch {
            val focusTimes = database.getAllFocusTimes()
            val tasks = taskRepository.getAllTasks()  // NEW: Load all tasks for mapping

            _focusTimes.value = focusTimes  // For now, just show FocusTime. Add mapping later.
        }
    }
}
```

**Note**: TaskRepository needs to be added to AppModule. For now, just load FocusTime directly.

---

## Implementation Order

### Phase 1: Data Layer (Steps 1-3)
1. Update AppDatabase.sq - Add taskId column (15 min)
2. Update focus.kt - Add taskId to models (10 min)
3. Update mappers.kt - Add taskId to mapper (5 min)
4. Update Database.kt - Add taskId parameter (10 min)

### Phase 2: Service Layer (Steps 4-5)
5. Create FocusTaskService.kt - NEW FILE (15 min)
6. Update AppModule.kt - Register service (5 min)

### Phase 3: Integration Layer (Steps 7-8)
7. Update TimerViewModel.kt - Save taskId (10 min)
8. Update TaskListScreen.kt - Focus button UI (15 min)

### Phase 4: History Layer (Step 8)
9. Update HistoryViewModel.kt - Load tasks (20 min) - DEFER if TaskRepository not available

### Testing
10. **TEST ALL PHASES** - Run app, test focus button, verify taskId saved (15 min)

**Total estimated time**: ~110 minutes

---

## Testing Checklist

### Data Layer
- [ ] AppDatabase.sq compiles with taskId column
- [ ] focus.kt compiles with taskId field
- [ ] Database.kt compiles with taskId parameter
- [ ] mappers.kt compiles with taskId in lambda
- [ ] FocusTime objects include taskId (null for existing, set for new)

### Service Layer
- [ ] FocusTaskService compiles and injects correctly
- [ ] FocusTaskService.getFocusedTaskId() returns correct ID
- [ ] FocusTaskService.setFocusedTask() updates StateFlow
- [ ] TimerViewModel successfully injects FocusTaskService

### UI Layer
- [ ] Focus button appears in task list
- [ ] Focus button highlights when task is focused (CenterFocusStrong icon, primary color)
- [ ] Clicking focus button updates FocusTaskService state
- [ ] Multiple tasks can have only one focused at a time
- [ ] App compiles and runs

### Integration
- [ ] TimerViewModel saves focus session with taskId
- [ ] Database contains taskId for focus sessions
- [ ] Focus session clears focused task after saving
- [ ] Focus button in TimerView (if added) shows current task

---

## Files to Modify

### Phase 1: Data Layer
1. `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq` - Add taskId column
2. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt` - Add taskId field
3. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/mappers.kt` - Add taskId parameter
4. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/Database.kt` - Add taskId parameter

### Phase 2: Service Layer
5. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` - NEW FILE
6. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt` - Register service

### Phase 3: Integration
7. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerViewModel.kt` - Save taskId
8. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt` - Add focus button UI

### Phase 4: History (OPTIONAL - Can defer)
9. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt` - Add task loading

---

## Risk Mitigation

### Database Schema
- **Risk**: Adding column breaks existing code
- **Mitigation**: Column is nullable, maintains backward compatibility
- **Verification**: Test on clean database first (new install)

### Task Loading
- **Risk**: Loading all tasks on every history load
- **Mitigation**: Cache tasks in ViewModel, implement pagination later
- **Alternative**: Load tasks on-demand, not in init

### UI State
- **Risk**: FocusTaskService is singleton, shared across screens
- **Mitigation**: StateFlow ensures all screens get same state
- **Alternative**: Consider scoped service per screen (not needed now)

---

## Notes for Developer

- **BACKWARD COMPATIBILITY**: taskId is nullable, existing FocusTime records work fine
- **MIGRATION**: ALTER TABLE is safe for nullable column
- **TESTING**: Test with clean database first, then with existing data
- **PERFORMANCE**: Task list caching may be needed for large datasets
- **DEFERRED**: History task display (13.5) can be implemented separately if time-constrained

**Estimated time**: 110 minutes = 1 hour 50 minutes

**READY FOR DEVELOPER** - All code snippets provided, file-by-file instructions
