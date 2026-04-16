# 001: Glimmer Basic UI Components - Findings

## Test Results
- All static checks (T1-T7) PASS
- Emulator verification: static analysis only (no Gradle environment)

## Review Results
- Score: 10/10 (PASS) — Post-feedback #3 review
- UX Fitness: 3/3, Code Quality: 3/3, Reusability: 2/2, Documentation: 2/2

## Human Feedback Resolution Summary

### Feedback #1 (2026-04-06): Manifest/Display Control Bug
- **Status**: RESOLVED
- **Fix**: 2-activity architecture (MainActivity + GlassesMainActivity)

### Feedback #2 (2026-04-06 20:13:40): UI Re-display + Text Centering
- **Status**: RESOLVED
- **Fix**: singleTop + onNewIntent + onResume reinit; VerticalList horizontalAlignment

### Feedback #3 (2026-04-16 00:56:08): Mojibake
- **Status**: RESOLVED
- **Fix**: All Japanese text replaced with English per reviewer direction
- **Root cause**: Japanese characters displayed as garbled text (mojibake) on emulator
- **Lesson learned**: Use English for all UI-visible text to avoid encoding issues on XR emulator

## Component Behavior Notes

### Glimmer Components
1. **GlimmerTheme**: `setContent { GlimmerTheme { ... } }` enables focus system automatically
2. **VerticalList + items**: `VerticalList { items(list) { ... } }` for list display. Never use LazyColumn
3. **VerticalList centering**: Default is Alignment.Start. Must set `horizontalAlignment = Alignment.CenterHorizontally`
4. **Focus navigation**: Touchpad DPAD_DOWN/UP auto-moves outline focus (no focusRequester needed)
5. **Card**: 3 slots — `title`, `action`, `content`. Buttons in both action and content slots
6. **Button**: Medium and Large sizes confirmed. `leadingIcon` slot for icon buttons
7. **TitleChip**: `leadingIcon` slot + content lambda for status bar display

### Projected API Integration
1. **2-activity architecture required**: MainActivity (LAUNCHER) -> ProjectedContext -> GlassesMainActivity (xr_projected)
2. **singleTop recommended**: Prevents UI re-display issues. onNewIntent reinitializes DisplayController
3. **4-layer lifecycle management**: onDestroy (observer) + onNewIntent (singleTop) + onResume (null check) + onStop (isFinishing)
4. **ProjectedDisplayController**: FLAG_KEEP_SCREEN_ON prevents display snooze. PresentationMode.VISUALS_ON monitoring
5. **FLAG_ACTIVITY_CLEAR_TOP + SINGLE_TOP**: Forces existing instance reuse on launch from MainActivity
6. **API 36 requirement**: isProjectedDeviceConnected requires API 36+. Guard with Build.VERSION.SDK_INT check

### Text Encoding
- **Use English text for UI display** to avoid mojibake on XR emulator
- Japanese text in code comments is generally safe but was also converted to English for consistency
- `Color.Black` = transparent on see-through display (additive light)
- Recommended padding: `horizontal=24.dp, vertical=16.dp`

## Suggestions for Future Experiments
- Voice input (Gemini Live) integration untested — verify in voice tickets
- Touchpad tap (DPAD_CENTER) for ListItem selection confirmed working
- Glimmer's `calculateContentColor` unused — consider for dynamic theme switching
- ProjectedActivityCompat for camera button input — verify in input tickets
- **Critical**: GlassesMainActivity robust lifecycle pattern must be followed in all experiments
