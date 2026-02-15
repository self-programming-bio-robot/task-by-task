# Task-by-Task Feature Progress

This document is used as a living checklist and progress tracker for the development of the project.

- Each feature is listed as a separate section with its own subtasks.
- Subtasks are marked with checkboxes for tracking progress. Use `[x]` to mark as done and `[ ]` to mark as pending.
- Subtasks are numbered as `X.Y` where `X` is the feature number and `Y` is the subtask number.
- When all subtasks in a feature are completed, the feature title is marked with a ✅.
- Feel free to add new features, split/merge subtasks, or update progress as you work.

---

# Features

## 1. Chat with buddy on day review screen ✅
- [x] 1.1 UI for chat
- [x] 1.2 Message sending logic
- [x] 1.3 Integration with review data
- [x] 1.4 Notification on new message

## 2. Detail day review screen with list of notes ✅
- [x] 2.1 UI for notes list in a extra pane

## 3. Add a start of focus time, to calculate pause time ✅
- [x] 3.1 Track focus start
- [x] 3.2 Calculate total pause time((current time - start of focus time) - timer duration)
- [x] 3.3 Save total pause time

## 4. Choosing timer type on start ✅
- [x] 4.1 UI for timer type selection
- [x] 4.2 Save selection

## 5. Infinite timer ✅
- [x] 5.1 Infinite timer logic
- [x] 5.2 UI for infinite timer

## 6. Adaptive navigation ✅
- [x] 6.1 Implement adaptive navigation

## 7. Statistics viewer ✅
- [x] 7.1 Design statistics screen layout
- [ ] 7.2 Implement graph for focus time (day per hour, week per day, month per day)
- [ ] 7.3 Implement graph for work cycles (day per hour, week per day, month per day)
- [ ] 7.4 Implement graph for created tasks (day per hour, week per day, month per day)
- [ ] 7.5 Implement graph for done tasks (day per hour, week per day, month per day)
- [x] 7.6 Add period selection (day/week/month)
- [x] 7.7 Integrate statistics data source
- [x] 7.8 Add navigation to statistics screen

## 8. Get default timer from database on start ✅
- [x] 8.1 Read default timer from DB
- [x] 8.2 Apply timer on start

## 9. Change feedback view to separate screen ✅
- [x] 9.1 New feedback screen UI
- [x] 9.2 Route to feedback screen

## 10. Fix bug: finish day button should review day with shift ✅
- [x] 10.1 Fix logic for finish day button

## 11. Abstract scheduler service and implementation for desktop ✅
- [x] 11.1 Abstract scheduler service
- [x] 11.2 Desktop implementation

## 12. Extract start of a day to the settings ✅
- [x] 12.1 Move start of day logic to settings(general settings)
- [x] 12.2 UI for setting start of day

## 13. Focus on task ✅
- [x] 13.1 Add button to task list for selecting a focus task from today's tasks
- [x] 13.2 Link selected task to focus time entity
- [x] 13.3 Update statistics logic to support focus-task linkage
- [x] 13.4 UI indication of focused task during focus session
- [x] 13.5 Display linked focus task in day statistics of focus time
