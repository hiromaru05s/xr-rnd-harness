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

## ProjectedActivityCompat Input Event Listening

**When to use**: Receiving hardware input events (camera button, etc.) from AI glasses
**Prerequisites**: `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`

```kotlin
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var projectedActivityCompat: ProjectedActivityCompat? = null

    private fun startListeningForInputEvents() {
        lifecycleScope.launch {
            try {
                val compat = ProjectedActivityCompat.create(this@GlassesMainActivity)
                projectedActivityCompat = compat

                // projectedInputEvents is a Flow<ProjectedInputEvent>
                compat.projectedInputEvents.collect { event ->
                    val actionName = event.inputAction.toString()
                    // Handle the event (e.g., TOGGLE_APP_CAMERA)
                }
            } catch (e: Exception) {
                // Handle connection failure
            }
        }
    }

    // IMPORTANT: Clean up in onDestroy
    private fun releaseResources() {
        projectedActivityCompat?.let { compat ->
            try { compat.close() } catch (e: Exception) { /* log */ }
        }
        projectedActivityCompat = null
    }
}
```

**Gotchas**:
- `ProjectedActivityCompat` implements `AutoCloseable` - always call `close()` in onDestroy
- `create()` is a suspend function that connects to the projection service
- `projectedInputEvents` is a cold Flow - collection starts listening
- Currently defined action: `TOGGLE_APP_CAMERA` (camera button press)
- If the glasses disconnect, the Flow will complete or throw

**Source**: experiments/input/004-camera-button-input

---

## TTS + Touchpad Integrated Navigation

**When to use**: When combining voice feedback with touchpad gesture navigation for accessible UI
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`, Android TTS (built-in)

```kotlin
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.focusable
import androidx.xr.glimmer.onIndirectPointerGesture

// Activity: TTS setup + navigation state
private var tts: TextToSpeech? = null
private var currentIndex by mutableIntStateOf(0)
private val cards = listOf("Page 1" to "Content 1", "Page 2" to "Content 2")

// Initialize TTS in onCreate
tts = TextToSpeech(this) { status ->
    if (status == TextToSpeech.SUCCESS) {
        speakCurrentCard()
    }
}

private fun navigateForward() {
    if (currentIndex < cards.size - 1) {
        currentIndex++
        speakCurrentCard()
    }
}

private fun speakCurrentCard() {
    val (title, desc) = cards[currentIndex]
    tts?.speak("$title. $desc", TextToSpeech.QUEUE_FLUSH, null, "card_$currentIndex")
}

// Composable: gesture detection
Box(
    modifier = Modifier
        .onIndirectPointerGesture(
            onSwipeForward = onSwipeForward,
            onSwipeBackward = onSwipeBackward,
            onClick = onClick,
        )
        .focusable()
) { /* content */ }
```

**Gotchas**:
- `focusable()` MUST follow `onIndirectPointerGesture` for gesture detection to work
- `QUEUE_FLUSH` ensures only one utterance plays at a time
- Check PresentationMode.VISUALS_ON for dual-mode (visual+voice / voice-only)
- Clean up TTS in onDestroy: `tts?.stop(); tts?.shutdown()`
- Card count should be 3 or fewer for FOV constraint

**Source**: experiments/integration/011-voice-touchpad-integration

---

## Notification Queue + Touchpad Navigation

**When to use**: When managing a queue of items (notifications, messages, tasks) with touchpad navigation on AI glasses
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.xr.glimmer.onIndirectPointerGesture
import androidx.compose.ui.focus.focusTarget

// 1. Queue Manager with max size (FOV constraint: 3 items)
class NotificationQueueManager(private val maxSize: Int = 3) {
    private val queue = mutableListOf<NotificationItem>()
    private var currentIndex = 0

    fun addNotification(item: NotificationItem) {
        queue.add(0, item)  // Newest first
        if (queue.size > maxSize) { queue.removeAt(queue.lastIndex) }
        currentIndex = 0
    }

    fun getCurrentNotification(): NotificationItem? = queue.getOrNull(currentIndex)
    fun moveToNext(): NotificationItem? {
        if (currentIndex < queue.size - 1) currentIndex++
        return getCurrentNotification()
    }
    fun moveToPrevious(): NotificationItem? {
        if (currentIndex > 0) currentIndex--
        return getCurrentNotification()
    }
    fun dismissCurrent(): NotificationItem? {
        if (queue.isEmpty()) return null
        queue.removeAt(currentIndex)
        if (currentIndex >= queue.size && currentIndex > 0) currentIndex--
        return getCurrentNotification()
    }
}

// 2. Touchpad gesture integration
Box(
    modifier = Modifier
        .onIndirectPointerGesture(
            onSwipeForward = { navigateForward() },   // Next item
            onSwipeBackward = { navigateBackward() },  // Previous item / re-read
            onClick = { dismissCurrent() },             // Dismiss
        )
        .focusTarget()  // Required for gesture detection
) { /* Card showing current item with index/total counter */ }
```

**Gotchas**:
- Queue maxSize should be 3 (FOV constraint for AI glasses)
- Newest items at index 0 (users expect latest notification first)
- dismissCurrent() must adjust currentIndex when removing from middle
- focusTarget() is mandatory after onIndirectPointerGesture
- Show position indicator (e.g., "2/3") on Card subtitle for orientation

**Source**: experiments/integration/017-notification-voice-bridge

---
