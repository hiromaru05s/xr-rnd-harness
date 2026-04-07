# 002: Touchpad Gesture Navigation - Findings

## Test Results
- All static checks (T1-T7) PASS
- Emulator: Static checks only (no Gradle environment)

## Review Results
- Score: 10/10 (PASS)
- UX Fitness: 3/3, Code Quality: 3/3, Reusability: 2/2, Documentation: 2/2

## Key Findings

### onIndirectPointerGesture Behavior
1. **focusTarget() is mandatory**: onIndirectPointerGesture requires .focusTarget()
   modifier to be chained. Without it, gesture callbacks are never triggered.
2. **Swipe direction**: onSwipeForward = downward swipe on touchpad (next),
   onSwipeBackward = upward swipe (previous). This maps to natural vertical navigation.
3. **onClick**: Single tap on touchpad. Useful for mode switching or selection.
4. **Scope**: Applied to a Box that wraps the entire screen, so gestures work
   anywhere within the UI area.

### VerticalStack + StackState
1. **Programmatic control**: StackState provides animateScrollToItem(index) for
   smooth animated transitions between cards.
2. **Card flip animation**: Glimmer handles the flip animation automatically.
3. **Key parameter**: Using key = { title } ensures stable identity for cards.
4. **items() function**: Works the same as VerticalList items() but produces
   stacked cards instead of a scrollable list.

### VerticalList in Gesture Context
1. **Auto focus management**: Glimmer DPAD_DOWN/UP automatically moves focus
   between ListItems. No manual focus management needed.
2. **Gesture interaction**: When both onIndirectPointerGesture and VerticalList
   coexist, the gesture handler fires first. DPAD focus movement still works
   independently through Glimmer system.

### Mode Switching Pattern
1. **Enum-based modes**: NavigationMode sealed enum with displayName property.
2. **when expression**: Kotlin exhaustive when ensures all modes are handled.
3. **Click to toggle**: Simple and intuitive for 2-mode switching.

## Implications for Next Experiments
- Voice input (Gemini Live) could add a third input modality alongside touchpad
- Camera button input via ProjectedInputAction.TOGGLE_APP_CAMERA is untested
- Combined gesture + voice navigation could be the next step
- VerticalStack is suitable for wizard/onboarding flows in glasses apps
