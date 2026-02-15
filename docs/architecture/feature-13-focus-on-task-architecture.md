# Architecture Plan: Feature 13 - Focus on Task

## Current State Analysis

### Data Models
**FocusTime** (`shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt`)
- Has fields: id, duration, feedback, finishedAt, startedAt, pauseTime
- **MISSING**: No `taskId` field to link to Task entity

**CreateFocusTime** (same file, line 16)
- Has fields: duration, feedback, finishedAt, startedAt, pauseTime
- **MISSING**: No `taskId` field

**Task** (`shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/task.kt`, line 8)
- Has fields: id, title, description, createdAt, completedAt, isCompleted, isToday
- All required fields present for task linkage

### Database Schema

**FocusTime Table** (`shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq`, line 3)
- Columns: id, duration, feedback, finishedAt, startedAt, pauseTime
- **MISSING**: No `taskId` column

### Repository Layer
**TaskRepository**: Does NOT exist
- Each service manages its own database queries
- No centralized task loading/caching
- **IMPACT**: Adding FocusTaskService requires TaskRepository for task lookup

### Service Layer

**FocusTaskService**: Does NOT exist
- Currently managed through TaskListViewModel directly
- **IMPACT**: Need centralized service for state management across screens

### UI Layer

**TaskListScreen**: Has focus button with CenterFocusStrong/Weak icons (line 177)
- Focus button toggles focus state (13.1 ✅)
- Uses FocusTaskService for state (NEED)

**TimerView**: Does NOT show focused task
- No visual indication of currently focused task (13.4 ❌)

**HistoryScreen**: Does NOT show linked tasks
- No integration with Task data (13.5 ❌)

---

## Architecture Plan

### Phase 1: Database Schema Migration (CRITICAL)

**Risk**: Data loss if not done correctly
**Approach**: Safe migration using new table + copy + rename

**Files**:
1. `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq`
   - Add `taskId INTEGER` to FocusTime table definition
   - Update `insertFocusTime` to include taskId parameter

2. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/cache/Database.kt`
   - Update `addFocusTime` overloads to pass taskId

3. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt`
   - Add `taskId: Long? = null` to FocusTime
   - Add `taskId: Long? = null` to CreateFocusTime

### Phase 2: Create TaskRepository (NEW)

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/repository/TaskRepository.kt` (NEW)

**Purpose**: Centralize task data access with caching

**Implementation**:
```kotlin
interface TaskRepository {
    fun getAllTasks(): List<Task>
    fun getTaskById(id: Long): Task?
}

class TaskRepository(
    private val database: Database
) : TaskRepository {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    val allTasks = _tasks.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        val tasks = database.taskRepository.getAllTasks()
        _tasks.value = tasks
    }
}
```

**DI**: Add to AppModule and inject where needed

### Phase 3: Create FocusTaskService

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` (NEW)

**Purpose**: Centralized state management for focused task

**Implementation**:
```kotlin
class FocusTaskService(
    private val database: Database
    private val taskRepository: TaskRepository
) {
    private val _focusedTask = MutableStateFlow<Task?>(null)
    val focusedTask: StateFlow<Task?> = _focusedTask.asStateFlow()

    init {
        // Load focused task from database on app start
        loadFocusedTask()
    }

    private fun loadFocusedTask() {
        val savedTaskId = database.settingRepository.getSetting<Long>(SettingKey.FOCUSED_TASK_ID)
        savedTaskId?.let { taskId ->
            val task = taskRepository.getTaskById(taskId)
            _focusedTask.value = task
        }
    }

    fun setFocusedTask(task: Task?) {
        _focusedTask.value = task
        // Save to database for persistence
        task?.let {
            database.settingRepository.saveSetting(SettingKey.FOCUSED_TASK_ID, it.id)
        }
    }

    fun clearFocusedTask() {
        _focusedTask.value = null
        // Clear saved setting
        database.settingRepository.saveSetting(SettingKey.FOCUSED_TASK_ID, null)
    }

    fun getFocusedTaskId(): Long? = _focusedTask.value?.id
}
```

**New Setting Key**: Add to SettingKey enum
```kotlin
enum class SettingKey(val id: Long) {
    OPENAI_TOKEN(1),
    THEME(2),
    START_OF_DAY_HOUR(3),
    FOCUSED_TASK_ID(4),  // NEW
}
```

### Phase 4: Update FocusTime Services

**File**: `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/repository/FocusTimeRepository.kt` (NEW)

**Purpose**: Separate focus time queries from generic Database class

**Implementation**:
```kotlin
interface FocusTimeRepository {
    fun addFocusTime(focusTime: CreateFocusTime)
    fun getFocusTimesWithTasks(tasks: Map<Long, Task>): List<FocusTimeWithTask>
}

class FocusTimeRepository(
    private val database: Database,
    private val taskRepository: TaskRepository
) : FocusTimeRepository {
    fun addFocusTime(focusTime: CreateFocusTime) {
        database.addFocusTime(focusTime)
    }

    fun getFocusTimesWithTasks(tasks: Map<Long, Task>): List<FocusTimeWithTask> {
        val focusTimes = database.getAllFocusTimes()
        return focusTimes.map { focusTime ->
            FocusTimeWithTask(
                focusTime = focusTime,
                task = focusTime.taskId?.let { taskId ->
                    tasks[taskId]
                }
            )
        }
    }
}
```

### Phase 5: Update HistoryViewModel

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt`

**Changes**:
- Add TaskRepository dependency
- Change state type from `List<FocusTime>` to `List<FocusTimeWithTask>`
- Map focus times to tasks
- Cache tasks in ViewModel

### Phase 6: Update HistoryScreen UI

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt`

**Changes**:
- Update to use `FocusTimeWithTask` items
- Display task info when available
- Show appropriate icon and styling

### Phase 7: Update TimerViewModel

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerViewModel.kt`

**Changes**:
- Add FocusTaskService dependency
- Save focusedTaskId when finishing session
- Clear focused task after saving (optional)

### Phase 8: Update TaskListScreen

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt`

**Changes**:
- Add FocusTaskService dependency
- Focus button visual feedback (icons, colors)
- Use FocusTaskService for state management

### Phase 9: Update Timer UI

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerView.kt`

**Changes**:
- Add FocusTaskService dependency
- Display focused task when running
- Show task title, duration tracking

### Phase 10: Update DaySummaryService

**File**: `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/DaySummaryService.kt`

**Changes**:
- Enhance reviewDay() to include task information
- "Today I focused on [task name] and worked for X minutes"

---

## Implementation Order

1. Database Schema Migration (15 min)
2. Create TaskRepository (10 min)
3. Create FocusTaskService (15 min)
4. Create FocusTimeRepository (10 min)
5. Update HistoryViewModel (10 min)
6. Update HistoryScreen UI (10 min)
7. Update TimerViewModel (10 min)
8. Update TaskListScreen (10 min)
9. Update Timer UI (15 min)
10. Update DaySummaryService (10 min)
11. Testing (15 min)

**Total estimated time**: ~2 hours

---

## Risk Mitigation

### Data Loss Risk
- **Mitigation**: Use nullable taskId, test on clean database first
- **Fallback**: Keep existing data migration path available

### Performance
- **Risk**: Loading all tasks on every history load
- **Mitigation**: Implement lazy loading or pagination
- **Alternative**: Load tasks on demand, cache in service layer

### State Synchronization
- **Risk**: Multiple sources of truth for focused task (service vs UI)
- **Mitigation**: Single source of truth (FocusTaskService), UI follows service state
- **Persistence**: Save to database on change, load on app start

### Backward Compatibility
- **Risk**: Existing FocusTime records without taskId
- **Mitigation**: taskId is nullable, UI shows "No task" or gracefully handles

---

## Files to Create/Modify

1. `shared/src/commonMain/sqldelight/dev/zhdanov/apps/shared/cache/AppDatabase.sq` - Add taskId column
2. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/SettingKey.kt` - Add FOCUSED_TASK_ID
3. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/model/focus.kt` - Add taskId fields
4. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/repository/TaskRepository.kt` - NEW FILE
5. `shared/src/commonMain/kotlin/dev/zhdanov/apps/shared/repository/FocusTimeRepository.kt` - NEW FILE
6. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/FocusTaskService.kt` - NEW FILE
7. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/di/AppModule.kt` - Register services
8. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryViewModel.kt` - Update
9. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/history/HistoryScreen.kt` - Update
10. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerViewModel.kt` - Update
11. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt` - Update
12. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerView.kt` - Update
13. `composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/services/DaySummaryService.kt` - Enhancement

---

## Testing Strategy

### Unit Tests
```kotlin
class FocusTaskServiceTest {
    @Test
    fun `setFocusedTask updates database`() {
        val service = FocusTaskService(mockDatabase, mockTaskRepository)
        val task = Task(id = 1, title = "Test")
        service.setFocusedTask(task)
        verify(mockDatabase).settingRepository.saveSetting(SettingKey.FOCUSED_TASK_ID, 1)
    }
}
```

### Integration Tests
1. Select task in TaskList
2. Verify focus state updates
3. Start timer, verify taskId is saved
4. Finish session, verify link in database
5. Check history shows linked task

---

## Notes

**Minimum Viable Product**: Complete subtasks 13.2, 13.3, 13.4
- Task linkage (13.2)
- UI indication (13.4)
- Statistics enhancement (13.3)

**Nice to Have**: Display linked tasks in history (13.5)

**Data Integrity**: taskId references Task.id but no foreign key
- Allows tasks to be deleted without breaking history
- Shows "Unknown task" for deleted tasks

**Performance**: Consider task list caching in FocusTaskService
- For large datasets, implement pagination or lazy loading

**Estimated Developer Time**: ~2 hours for core functionality
