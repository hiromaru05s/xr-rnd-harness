# 001: Glimmer Basic UI Components

## Hypothesis
Verify that GlimmerTheme's basic components (Button, Card, ListItem, VerticalList, TitleChip) render correctly on a black background designed for see-through display, and that touchpad focus navigation functions properly.

## Technologies Used
- Skills: glimmer-api, glasses-arch, projected-api
- Key libraries:
  - androidx.xr.glimmer:glimmer:1.0.0-alpha08
  - androidx.xr.projected:projected:1.0.0-alpha05
  - androidx.xr.runtime:runtime:1.0.0-alpha12
- Compose BOM: 2025.01.00
- Kotlin + Compose + Gradle Kotlin DSL (compileSdk=36, minSdk=35)

## Human Feedback History

### Feedback #1 (2026-04-06): Manifest/Display Control Bug
- **Issue**: UI visible only on first launch; invisible after navigating away
- **Root cause**: Missing 2-activity architecture, no DisplayController management
- **Fix**: Added MainActivity as LAUNCHER, ProjectedContext-based launch, DisplayController lifecycle

### Feedback #2 (2026-04-06 20:13:40): UI Re-display + Text Centering
- **Issue**: UI disappears after screen switch and never comes back in same session; text slightly off-center
- **Root cause**: DisplayController not reinitialized on lifecycle transitions; VerticalList default alignment is Start
- **Fix**: singleTop + onNewIntent reinit + onResume null-check; VerticalList horizontalAlignment = CenterHorizontally

### Feedback #3 (2026-04-16 00:56:08): Mojibake (Character Encoding)
- **Issue**: Japanese text displayed as garbled characters (mojibake) on emulator
- **Direction from reviewer**: "Replace with English"
- **Fix**: All UI-visible text in BasicUiScreen, MainActivity, and test files changed from Japanese to English. Code comments also converted to English for consistency.

## Implementation
Two-activity architecture: `MainActivity` (phone launcher) -> `ProjectedContext` -> `GlassesMainActivity` (glasses display).
GlassesMainActivity wraps `BasicUiScreen` in `GlimmerTheme`.
Components showcased on a single screen:
- `TitleChip`: shows selected item status
- `Card` + `Button` (Medium/Large with icon): action controls
- `VerticalList` + `ListItem` x3: touchpad focus navigation demo

## How to Run
1. Open `experiments/ui/001-glimmer-basic-ui/` in Android Studio Canary
2. Start phone AVD and AI glasses AVD, pair them
3. Run `MainActivity` targeting the phone AVD (launches as LAUNCHER)
4. GlassesMainActivity auto-appears on glasses display when connected
5. Tap "Show on Glasses" button for manual launch
6. Verify BasicUiScreen renders on glasses AVD
7. Test touchpad navigation: focus outline moves between components
8. **Re-display test**: Navigate away and re-launch; verify UI appears again

## Findings
(Updated after test and review)

## Extracted Patterns
- [patterns/ui-patterns.md](../../patterns/ui-patterns.md) - Glimmer basic component layout pattern
- [patterns/architecture-patterns.md](../../patterns/architecture-patterns.md) - 2-activity launch pattern
