# Input Patterns

> AI-readable patterns for touchpad, voice, and camera button input on AI glasses.
> Code snippets are copy-paste ready with full imports.

---

## Touchpad Gesture Detection with onIndirectPointerGesture

**When to use**: Detecting touchpad swipe and click gestures on AI glasses
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.xr.glimmer.onIndirectPointerGesture

// Apply to any composable that should detect touchpad gestures
// IMPORTANT: .focusTarget() MUST be chained after .onIndirectPointerGesture()
Box(
    modifier = Modifier
        .onIndirectPointerGesture(
            onSwipeForward = { /* touchpad downward swipe = next */ },
            onSwipeBackward = { /* touchpad upward swipe = previous */ },
            onClick = { /* touchpad single tap */ },
        )
        .focusTarget() // Required! Without this, gestures will not fire
)
```

**Gotchas**:
- `.focusTarget()` is mandatory. Without it, no gesture callbacks are triggered
- Swipe forward = downward swipe on touchpad (natural direction for "next")
- Swipe backward = upward swipe on touchpad (natural direction for "previous")
- Apply to the outermost container for screen-wide gesture detection

**Source**: experiments/input/002-touchpad-navigation

---

## VerticalStack Card Navigation with Touchpad

**When to use**: Card-based page navigation with touchpad swipe for AI glasses
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalStack
import androidx.xr.glimmer.list.rememberStackState
import androidx.xr.glimmer.onIndirectPointerGesture
import kotlinx.coroutines.launch

@Composable
fun CardNavigationExample() {
    val stackState = rememberStackState()
    var currentIndex by remember { mutableIntStateOf(0) }
    val totalCards = 3
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .onIndirectPointerGesture(
                onSwipeForward = {
                    val next = (currentIndex + 1).coerceAtMost(totalCards - 1)
                    if (next != currentIndex) {
                        currentIndex = next
                        coroutineScope.launch {
                            stackState.animateScrollToItem(currentIndex)
                        }
                    }
                },
                onSwipeBackward = {
                    val prev = (currentIndex - 1).coerceAtLeast(0)
                    if (prev != currentIndex) {
                        currentIndex = prev
                        coroutineScope.launch {
                            stackState.animateScrollToItem(currentIndex)
                        }
                    }
                },
            )
            .focusTarget()
    ) {
        VerticalStack(state = stackState) {
            items(totalCards, key = { it }) { index ->
                Card(title = { Text("Page ${index + 1}") }) {
                    Text("Content for page ${index + 1}")
                }
            }
        }
    }
}
```

**Gotchas**:
- Use `coerceAtMost(totalCards - 1)` and `coerceAtLeast(0)` for safe bounds
- `animateScrollToItem()` is a suspend function - launch in coroutine scope
- Track currentIndex separately from StackState for reliable position tracking
- Keep to 3 cards max for glasses FOV constraint

**Source**: experiments/input/002-touchpad-navigation

---
