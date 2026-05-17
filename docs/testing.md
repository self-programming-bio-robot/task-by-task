# Testing

This document tracks the test suites that exist in the project, what they protect, and how to run them. Keep it updated whenever a test class is added, removed, or its coverage changes materially.

## Commands

```powershell
# Preferred check for the currently supported runtime.
.\gradlew.bat :composeApp:desktopTest --console=plain

# Shared data/model tests on JVM.
.\gradlew.bat :shared:jvmTest --console=plain

# All configured tests for all modules/targets that are available on the machine.
.\gradlew.bat test --console=plain
```

Desktop is the currently supported runtime. Android and iOS UI tests are intentionally not documented as supported yet because their platform DI/database wiring is not complete.

## composeApp Tests

| Test class | Source set | Type | What it verifies |
| --- | --- | --- | --- |
| `FocusTaskServiceTest` | `composeApp/src/commonTest` | Unit | Focused-task state management: setting, clearing, reading focused task ids, initial null state, and replacing one focused task with another. |
| `TaskComponentsUiTest` | `composeApp/src/commonTest` | Compose UI component | Task input and task-row behavior: trimming and clearing new-task input, ignoring blank submissions, row click callbacks, completion checkbox callbacks, focus button enabled/disabled states, and today-toggle callbacks. |
| `WorkspaceSessionServiceTest` | `composeApp/src/desktopTest` | Service integration with in-memory DB | Workspace encryption lifecycle: encrypted task storage, lock/unlock behavior, invalid PIN handling, disabling encryption back to plaintext, per-workspace assistant config, and fresh workspace lock state. |
| `DaySummaryServiceTest` | `composeApp/src/desktopTest` | Service integration with in-memory DB | Finish-day behavior with configured start-of-day shift, linked task summaries through focus-session junction rows, fake review client response persistence, and missing OpenAI token failure behavior. |
| `ChatServiceTest` | `composeApp/src/desktopTest` | Service integration with fake client | Chat session message flow: missing-token failure state and successful assistant response append through an injected fake chat client. |
| `AppDesktopSmokeUiTest` | `composeApp/src/desktopTest` | Desktop Compose UI smoke | Starts `App()` with in-memory Koin fixtures, verifies Home loads, navigates to Tasks, adds a task through real UI, toggles Today, focuses the task, and confirms the flow returns to Home without touching the real database or network. |

### Desktop UI Test Fixtures

`DesktopUiTestFixtures.kt` provides the isolated Koin graph for desktop UI tests:

- In-memory SQLite via `JdbcSqliteDriver.IN_MEMORY`.
- No-op scheduler.
- Fake theme service.
- Fake review/chat clients.
- Test dispatchers.
- `stopKoin()` cleanup after each test.

Use these fixtures for future desktop smoke/UI integration tests instead of the production desktop module, so tests do not write to `~/.taskByTask/cache.db` or call external services.

## shared Tests

| Test class | Source set | Type | What it verifies |
| --- | --- | --- | --- |
| `TimerTest` | `shared/src/commonTest` | Unit | Deterministic timer behavior: Pomodoro work-stage finish/ticks and infinite timer reset behavior. |
| `DateUtils` | `shared/src/commonTest` | Unit | Start-of-day calculation across shifts and time zones, plus `LocalTime.toDuration()` conversion. |
| `DatabaseTest` | `shared/src/jvmTest` | Repository/database integration | In-memory SQLDelight behavior: focus sessions linked to multiple tasks, day summary task snapshots, structured settings round trips, workspace task isolation, workspace icon updates, legacy OpenAI token migration, and legacy schema compatibility columns. |

## Maintenance Rules

- Add new tests to the relevant table in this file in the same change that introduces them.
- Prefer source-set names in docs (`commonTest`, `desktopTest`, `jvmTest`) so it is clear where the test runs.
- For Compose UI tests, prefer stable tags from `UiTestTags` for actions and use visible text mainly for user-facing assertions.
- Keep desktop integration tests on in-memory DB/fake clients unless the test is explicitly intended to exercise production platform wiring.
