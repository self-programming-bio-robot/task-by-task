# Task Linking UI - Material Design 3 Specifications

## Overview
Design specifications for task-to-focus-time linking UI components in Task-by-Task desktop application.

---

## 1. TaskItem Toggle Button (TaskListScreen)

### Location
`composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/screens/tasks/TaskListScreen.kt:153-187`

### Layout Structure
```
Row (horizontal)
├── Checkbox (existing)
├── Spacer (16.dp)
├── Text (task.title) - weight(1f)
├── Spacer (16.dp)
├── FocusToggleButton (NEW)
└── IconToggleButton (Today - existing)
```

### Component Specifications

#### FocusToggleButton (IconToggleButton)

**Visual Properties:**
| Property | Value |
|----------|-------|
| Icon | `Icons.Default.CenterFocusStrong` |
| Icon Size | `20.dp` |
| Touch Target | `48.dp` (min) |
| Container Size | `40.dp` |
| Border Shape | `CircleShape` or `MaterialTheme.shapes.small` |

**Color Scheme:**
| State | Container Color | Icon Color |
|-------|-----------------|------------|
| Unchecked | `Color.Transparent` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| Checked | `MaterialTheme.colorScheme.primaryContainer` | `MaterialTheme.colorScheme.primary` |
| Pressed | `MaterialTheme.colorScheme.primaryContainer` (alpha 0.8) | `MaterialTheme.colorScheme.primary` |
| Hovered (Desktop) | `MaterialTheme.colorScheme.primaryContainer` (alpha 0.5) | `MaterialTheme.colorScheme.primary` |
| Focused | `MaterialTheme.colorScheme.primaryContainer` + focus ring | `MaterialTheme.colorScheme.primary` |

**Typography:** N/A (icon only)

**Spacing:**
- Padding before Today button: `8.dp`
- Internal padding: `12.dp` (to achieve 40dp size with 20dp icon)

**Accessibility:**
- Content Description (Unchecked): "Focus on this task"
- Content Description (Checked): "Remove focus from this task"
- State Description: "Focused" / "Not focused"

**Interaction:**
- Toggle: Click to select/focus, click again to deselect
- Animation: `spring(stiffness = Spring.StiffnessMediumLow)`

**Implementation:**
```kotlin
@Composable
fun FocusToggleButton(
    isFocused: Boolean,
    onToggleFocus: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isFocused) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val iconTintColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    IconToggleButton(
        checked = isFocused,
        onCheckedChange = onToggleFocus,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = if (isFocused) "Remove focus" else "Focus on task",
            tint = iconTintColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
```

---

## 2. Linked Task Indicator (TimerView)

### Location
`composeApp/src/commonMain/kotlin/dev/zhdanov/apps/composeApp/components/timer/TimerView.kt`

### Position
Above the circular timer, between top of screen and TimerCircle:
```
Column
├── FocusedTaskIndicator (NEW - if task focused)
├── Spacer (8.dp)
├── TimerCircle (existing)
└── Control Buttons (existing)
```

### Component Specifications

#### FocusedTaskIndicator (Single Task)

**Visual Properties:**
| Property | Value |
|----------|-------|
| Icon | `Icons.Default.CenterFocusStrong` |
| Icon Size | `16.dp` |
| Background | `MaterialTheme.colorScheme.primaryContainer` |
| Shape | `MaterialTheme.shapes.small` (8.dp corner radius) |
| Elevation | 0.dp (flat surface) |
| Height | `40.dp` min |

**Layout:**
```
Surface (fillMaxWidth)
├── Row (horizontal, padding 8.dp)
│   ├── Icon (16.dp)
│   ├── Spacer (8.dp)
│   ├── Text (task.title) - weight(1f)
│   └── IconButton (Close, 24.dp)
```

**Color Scheme:**
| Element | Color |
|---------|-------|
| Container | `MaterialTheme.colorScheme.primaryContainer` |
| Icon (Focus) | `MaterialTheme.colorScheme.onPrimaryContainer` (70% opacity) |
| Text | `MaterialTheme.colorScheme.onPrimaryContainer` |
| Close Button | `MaterialTheme.colorScheme.onPrimaryContainer` (70% opacity) |

**Typography:**
| Property | Value |
|----------|-------|
| Style | `MaterialTheme.typography.labelMedium` |
| Font Size | `12.sp` (labelMedium default) |
| Max Lines | 1 |
| Overflow | `TextOverflow.Ellipsis` |
| Prefix | "Focusing on: " (optional, or just show title) |

**Spacing:**
| Position | Value |
|----------|-------|
| Horizontal Padding | `16.dp` (8.dp each side) |
| Vertical Padding | `8.dp` (4.dp top/bottom) |
| Margin around Surface | `horizontal 8.dp, vertical 4.dp` |
| Space before Timer | `8.dp` |

**Title Truncation:**
- Max characters: ~20-25 for visibility
- Always truncate with ellipsis: `maxLines = 1, overflow = TextOverflow.Ellipsis`

**Accessibility:**
- Semantic: `Role.Button` (for clear button)
- Content Description: "Currently focusing on {task title}. Tap to clear."
- State Description: "Focused task"

**Interaction:**
- Close button: Clears focused task
- Click on entire chip: Navigate to task details (optional enhancement)

**Implementation:**
```kotlin
@Composable
fun FocusedTaskIndicator(
    task: Task,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
                    .copy(alpha = 0.7f),
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
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear focused task",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                        .copy(alpha = 0.7f)
                )
            }
        }
    }
}
```

---

## 3. Multi-Task Indicator (TimerView)

### Use Case
When multiple tasks are linked to the focus session (future enhancement).

### Component Specifications

#### MultiTaskIndicator

**Visual Properties:**
| Property | Value |
|----------|-------|
| Same as single task | - |
| Badge | Additional pill/indicator showing count |

**Layout Options:**

**Option A: List Style (Recommended)**
```
Surface
├── Row
│   ├── Icon (CenterFocusStrong)
│   ├── Spacer
│   ├── Text ("Focusing on 3 tasks")
│   └── Close Button
```

**Option B: Chip Stack**
```
Surface
├── Row
│   ├── Chip (Task 1)
│   ├── Chip (Task 2)
│   ├── Chip (+1 more)
│   └── Close Button
```

**Text Format:**
- 2 tasks: "Focusing on: Task1, Task2"
- 3-5 tasks: "Focusing on: Task1, Task2, and 2 more"
- 5+ tasks: "Focusing on 5 tasks"

**Typography:**
| Property | Value |
|----------|-------|
| Style | `MaterialTheme.typography.labelMedium` |
| Max Lines | 2 (allow wrap for multiple tasks) |

**Color Scheme:** Same as single task indicator

**Badge (Optional):**
- Small circular badge: Surface dimension `20.dp`
- Background: `MaterialTheme.colorScheme.primary`
- Text: `MaterialTheme.colorScheme.onPrimary`
- Font: `labelSmall`
- Position: Overlapping the icon or at the end

---

## 4. Visual States & Animations

### FocusToggleButton States

| State | Visual Treatment |
|-------|------------------|
| Default | Transparent background, onSurfaceVariant icon |
| Focused (Keyboard) | Focus ring (`Primary` color, 2.dp) |
| Hovered | primaryContainer (50% alpha) |
| Pressed | primaryContainer (80% alpha), scale(0.95) |
| Selected | primaryContainer, primary icon |

### FocusedTaskIndicator States

| State | Visual Treatment |
|-------|------------------|
| Appearing | Slide in from top (offset -20.dp → 0), fade in |
| Disappearing | Slide out to top, fade out |
| Hovered (Chip) | SurfaceVariant overlay (5% alpha) |
| Clear Button Hovered | onError color (optional) |

### Animation Specifications

```kotlin
// Fade in/out
fadeInAnimation(
    animationSpec = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
)

// Slide animation
slideInVerticallyAnimation(
    initialOffsetY = { -20 },
    animationSpec = spring(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy
    )
)

// Toggle button
scaleAnimation(
    targetScale = if (pressed) 0.95f else 1f,
    animationSpec = spring(
        stiffness = Spring.StiffnessMediumLow
    )
)
```

---

## 5. Responsive Behavior

### Desktop (Mouse)
- Hover states enabled
- Larger click targets (48.dp min)
- Cursor pointer on interactive elements

### Compact (Mobile)
- Touch targets minimum 48.dp
- No hover states
- Haptic feedback on toggle (platform-specific)

---

## 6. Accessibility (A11Y)

### FocusToggleButton
- Minimum touch target: 48.dp
- Semantic actions: `OnClick`, `OnLongClick` (optional)
- State descriptions: "Focused" / "Not focused"
- Content description updates with state

### FocusedTaskIndicator
- Screen reader: "Focusing on {task title}"
- Clear button: "Remove focus from {task title}"
- Heading level: Not a heading (non-essential info)

### Color Contrast
- All text meets WCAG AA: 4.5:1 contrast ratio
- primaryContainer + onPrimaryContainer: M3 compliant
- Consider dark mode testing

---

## 7. Dark Mode Support

All color tokens use MaterialTheme.colorScheme, ensuring automatic dark mode support:

| Element | Light Mode | Dark Mode |
|---------|------------|-----------|
| primaryContainer | Tonal container (light) | Tonal container (dark) |
| onPrimaryContainer | High contrast light | High contrast dark |
| onSurfaceVariant | Muted dark | Muted light |

---

## 8. Design Tokens Summary

### Colors
```kotlin
// Primary (for selected/focused state)
val primary = MaterialTheme.colorScheme.primary
val onPrimary = MaterialTheme.colorScheme.onPrimary

// Primary Container (for background)
val primaryContainer = MaterialTheme.colorScheme.primaryContainer
val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

// Surface Variant (for unselected state)
val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

// Error (for clear button hover - optional)
val error = MaterialTheme.colorScheme.error
val onError = MaterialTheme.colorScheme.onError
```

### Typography
```kotlin
// Task title in indicator
val labelMedium = MaterialTheme.typography.labelMedium // 12sp, Medium weight

// Task title in list (existing)
val bodyLarge = MaterialTheme.typography.bodyLarge // 16sp, Regular weight
```

### Shapes
```kotlin
val small = MaterialTheme.shapes.small // 8.dp corner radius
val medium = MaterialTheme.shapes.medium // 12.dp corner radius
```

### Spacing
```kotlin
val spacingExtraSmall = 4.dp
val spacingSmall = 8.dp
val spacingMedium = 16.dp
```

---

## 9. Implementation Checklist

### TaskItem (TaskListScreen)
- [ ] Add FocusToggleButton component
- [ ] Inject FocusTaskService
- [ ] Add onToggleFocus callback to TaskItem
- [ ] Position between Spacer and Today button
- [ ] Test toggle behavior
- [ ] Test keyboard navigation

### TimerView
- [ ] Add FocusedTaskIndicator composable
- [ ] Inject FocusTaskService
- [ ] Conditionally show when focusedTask != null
- [ ] Position above TimerCircle
- [ ] Add clear button functionality
- [ ] Test title truncation
- [ ] Test enter/exit animations

### Multi-Task (Future)
- [ ] Add MultiTaskIndicator composable
- [ ] Implement count-based display logic
- [ ] Add horizontal scroll for many tasks
- [ ] Test with 2, 5, 10+ tasks

---

## 10. Reference Composables

### Material 3 Components Used
- `IconToggleButton` - Toggle button with icon
- `Surface` - Colored container
- `IconButton` - Icon-only button in Surface
- `Row`/`Column` - Layout containers

### Icons
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
```

---

## 11. Mockups (Text Description)

```
┌─────────────────────────────────────────────────────┐
│ Timer View                                           │
├─────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────┐ │
│ │ 🔍 Focusing on: Complete project documentation  ❌│ │ ← FocusedTaskIndicator
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│              ┌───────────────────┐                  │
│              │                   │                  │
│              │    25:00          │                  │
│              │      WORK         │                  │
│              │                   │                  │
│              └───────────────────┘                  │
│                                                     │
│              [Start]                                │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ Task List                                           │
├─────────────────────────────────────────────────────┤
│ [ ] Create API design                  [🔍] [📅]     │ ← TaskItem with Focus button
│ [✓] Write unit tests                    [🔍] [📅]     │
│ [ ] Update documentation               [🔍] [📅]     │
│                                                     │
│ ┌───────────────────────────────────────────────┐  │
│ │ Enter new task                        [+]     │  │
│ └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘

Legend:
[🔍] = FocusToggleButton (CenterFocusStrong icon)
[📅] = Today button (existing)
```

---

## 12. Testing Criteria

### Visual Tests
- [ ] Focus button shows selected state when task is focused
- [ ] Only one focus button can be selected at a time
- [ ] Focused task indicator appears above timer
- [ ] Task title truncates properly at ~20 chars
- [ ] Colors adapt to light/dark theme

### Interaction Tests
- [ ] Clicking focus button toggles focus state
- [ ] Clicking close button removes focused task
- [ ] Focus state persists across navigation
- [ ] Keyboard navigation works (Tab, Enter)

### Edge Cases
- [ ] Very long task titles (>100 chars)
- [ ] Empty task title
- [ ] Rapid toggle clicks
- [ ] Clear during active timer session

---

## 13. Notes for Developers

1. **Focus State Management**: Use `FocusTaskService` as single source of truth
2. **Icon Import**: Ensure `Icons.Default.CenterFocusStrong` is imported
3. **Spacing Consistency**: Use 8.dp spacing to match existing UI
4. **Color Tokens**: Always use MaterialTheme.colorScheme for theme support
5. **Animation Duration**: Keep transitions quick (200ms) for non-intrusive UX

---

## Document Version
- Version: 1.0
- Last Updated: 2025-02-15
- Designer: ui-designer-2
- Status: Ready for Implementation
