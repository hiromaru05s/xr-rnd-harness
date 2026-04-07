# 002: Touchpad Gesture Navigation

## Hypothesis
onIndirectPointerGesture can be used to implement touchpad swipe-based
VerticalStack card flipping and VerticalList scrolling.

## Technology Used
- Skills: glimmer-api, glasses-arch
- Libraries:
  - androidx.xr.glimmer:glimmer:1.0.0-alpha08
  - androidx.xr.projected:projected:1.0.0-alpha05
  - androidx.xr.runtime:runtime:1.0.0-alpha12
- Compose BOM: 2025.01.00
- Kotlin + Compose + Gradle Kotlin DSL (compileSdk=36, minSdk=35)

## Implementation
Two navigation modes switchable by touchpad click:

1. **Card Stack Mode**: VerticalStack with 3 cards. Swipe forward/backward
   to flip through cards. Each card has title, action button, and main button.
   StackState + animateScrollToItem() for smooth transitions.

2. **List View Mode**: VerticalList with 3 items. DPAD-based focus navigation
   provided by Glimmer auto-focus system. Click on ListItem to select.

Key components:
- `onIndirectPointerGesture`: Detects swipe forward, swipe backward, and click
- `.focusTarget()`: Required modifier for gesture detection to work
- `VerticalStack` + `StackState`: Card flip container with programmatic scroll
- `VerticalList`: Focus-based scrolling list
- `TitleChip` x2: Position indicator and action feedback display

Architecture:
- MainActivity (phone launcher) -> ProjectedContext -> GlassesMainActivity (glasses)
- Robust lifecycle pattern from experiment 001 applied

## How to Run
1. Open `experiments/input/002-touchpad-navigation/` in Android Studio Canary
2. Launch smartphone AVD and AI glasses AVD, pair them
3. Run with smartphone AVD as target (launches via LAUNCHER)
4. Glasses display shows TouchpadNavigationScreen
5. Swipe forward/backward on touchpad to flip cards
6. Click touchpad to switch between Card Stack and List View
7. In List View, use DPAD to navigate list items

## Findings
(To be filled after test and review)

## Extracted Patterns
(To be filled after pass)
