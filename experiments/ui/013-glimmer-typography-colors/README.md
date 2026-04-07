# 013: Glimmer Typography and Color System

## Hypothesis
Glimmer's 7 typography styles and 8-color system, along with Colors.copy() and Typography.copy(), can express readable information hierarchy on transparent display.

## Technologies
- Skills: glimmer-api
- Libraries: Glimmer (GlimmerTheme, Colors, Typography)

## Implementation
- All 7 typography styles displayed: titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall, caption
- All 8 colors displayed: primary, secondary, positive, negative, background, surface, outline, outlineVariant
- Colors.copy() for custom color theme (Orange primary, Cyan positive)
- Typography.copy() for custom font weight (titleLarge with FontWeight.Black)
- Touchpad swipe for 3-page navigation (Typography / Colors / Custom Theme)
- onIndirectPointerGesture + focusable() for gesture detection

## How to Run
1. Open in Android Studio Canary
2. Start Smartphone AVD and AI Glasses AVD
3. Target Smartphone AVD and run

## Findings
(After test/review)

## Extracted Patterns
(After PASS, reference patterns/ links)
