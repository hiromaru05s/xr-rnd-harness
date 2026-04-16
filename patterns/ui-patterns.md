# Glimmer UI Patterns

> This file is loaded as context by AI agents for vibe coding reference.
> Code snippets maintain copy-paste completeness.

---

## Glimmer Basic Component Layout

**When to use**: Building a basic Glimmer UI screen for AI glasses
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`, `implementation(platform("androidx.compose:compose-bom:2025.01.00"))`, `implementation("androidx.compose.material:material-icons-core")`

```kotlin
// === Activity: Minimal GlimmerTheme wrapper ===
package com.example.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.xr.glimmer.GlimmerTheme

class GlassesMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // GlimmerTheme wrapper enables the focus system automatically
            GlimmerTheme {
                MyScreen()
            }
        }
    }
}
```

```kotlin
// === Screen: Black background + basic components ===
package com.example.myapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items

@Composable
fun MyScreen(modifier: Modifier = Modifier) {
    var selectedLabel by remember { mutableStateOf("None") }

    // Black background = transparent on see-through display (additive light)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // TitleChip: status bar display
            TitleChip(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
                }
            ) {
                Text(selectedLabel)
            }

            // Card: title/action/content 3-slot structure
            Card(
                title = { Text("Actions") },
                action = {
                    // Medium-size Button
                    Button(
                        onClick = { selectedLabel = "Confirmed" },
                        buttonSize = ButtonSize.Medium,
                    ) { Text("OK") }
                }
            ) {
                // Large-size Button with icon
                Button(
                    onClick = { selectedLabel = "Running" },
                    buttonSize = ButtonSize.Large,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    }
                ) { Text("Run") }
            }

            // VerticalList: 3 items max (glasses FOV constraint)
            // DPAD_DOWN/UP auto-moves outline focus (no focusRequester needed)
            VerticalList {
                data class MenuItem(val label: String, val icon: ImageVector)
                val menuItems = listOf(
                    MenuItem("Alerts", Icons.Default.Notifications),
                    MenuItem("Settings", Icons.Default.Settings),
                    MenuItem("Help", Icons.Default.Info),
                )
                items(menuItems) { item ->
                    ListItem(
                        onClick = { selectedLabel = item.label },
                        leadingIcon = {
                            Icon(imageVector = item.icon, contentDescription = null)
                        },
                        supportingLabel = { Text("Tap to select") },
                    ) {
                        Text(item.label)
                    }
                }
            }
        }
    }
}
```

```xml
<!-- === AndroidManifest.xml: 2-activity architecture (officially recommended) === -->
<!-- Note: DO NOT add LAUNCHER category to GlassesMainActivity -->
<application>
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity
        android:name=".GlassesMainActivity"
        android:exported="true"
        android:requiredDisplayCategory="xr_projected"
        android:label="My Glass App">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
        </intent-filter>
    </activity>
</application>

**Gotchas**:
- Do NOT use `LazyColumn`. Always use `VerticalList` + `items()`
- Background must be `Color.Black` (transparent on additive-light see-through display)
- `android:requiredDisplayCategory="xr_projected"` is required for glasses projection
- `GlimmerTheme` wrapper is mandatory -- focus system will not work without it
- Lists must have 3 items or fewer (FOV 50-70 degree constraint)
- Glimmer `Text`, `Button`, `Icon` etc. are from `androidx.xr.glimmer` package (NOT Compose Material)
- **Use English text for UI labels** -- Japanese text causes mojibake on XR emulator

**Source**: experiments/ui/001-glimmer-basic-ui

---

## TitleChip Status Display

**When to use**: Display current state or mode in a single line at the top
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip

// TitleChip: leadingIcon slot + content lambda
TitleChip(
    leadingIcon = {
        Icon(imageVector = Icons.Default.Star, contentDescription = null)
    }
) {
    Text("Status Text")
}
```

**Gotchas**:
- TitleChip is display-only, not tappable
- leadingIcon is optional (omit the parameter to skip)

**Source**: experiments/ui/001-glimmer-basic-ui

---

## Card + Button Combination

**When to use**: Creating action cards with titled button groups
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text

// Card: title (top label), action (top-right action), content (body)
Card(
    title = { Text("Card Title") },
    action = {
        Button(
            onClick = { /* Actions */ },
            buttonSize = ButtonSize.Medium,
        ) { Text("Medium") }
    }
) {
    Button(
        onClick = { /* Actions */ },
        buttonSize = ButtonSize.Large,
        leadingIcon = {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        }
    ) { Text("Large") }
}
```

**Gotchas**:
- Only `ButtonSize.Medium` and `ButtonSize.Large` (Small has poor visibility on glasses)
- `leadingIcon` pairs best with Large size for readability

**Source**: experiments/ui/001-glimmer-basic-ui

## VerticalList Center-Alignment Pattern

**When to use**: Centering VerticalList items on screen (default is Alignment.Start = left-aligned)
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.ui.Alignment
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items

// Without explicit horizontalAlignment, default is Alignment.Start (left-aligned)
VerticalList(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    items(menuItems) { item ->
        ListItem(
            onClick = { /* Actions */ },
        ) {
            Text(item.label)
        }
    }
}
```

**Gotchas**:
- VerticalList default `horizontalAlignment` is `Alignment.Start`. Even if parent Column uses CenterHorizontally, VerticalList uses its own alignment
- Items inside VerticalList will be left-aligned even if parent Column/Box is center-aligned
- Center alignment is especially important on see-through displays (gaze focal point is center)

**Source**: experiments/ui/001-glimmer-basic-ui (discovered via human feedback fix #2)

---

## Surface/SurfaceDefaults Customization

**When to use**: Customizing Card/ListItem/Box borders and Surface settings
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.SurfaceDefaults
import androidx.xr.glimmer.surface

// 1. Card with custom border and color
Card(
    title = { Text("Custom Card") },
    color = GlimmerTheme.colors.surface,
    border = SurfaceDefaults.border(
        width = 3.dp,
        color = GlimmerTheme.colors.primary,
    ),
) { /* content */ }

// 2. Modifier.surface() for focusable Box
Box(
    modifier = Modifier
        .surface(
            focusable = true,
            shape = GlimmerTheme.shapes.small, // RoundedCornerShape(24.dp)
            color = GlimmerTheme.colors.surface,
            border = BorderStroke(2.dp, GlimmerTheme.colors.positive),
        )
        .padding(16.dp), // .surface() THEN .padding()
) { /* content */ }
```

**Gotchas**:
- `.surface().padding()` order matters. Reversed = border draws inside padding
- SurfaceDefaults.border() defaults to 2dp, expands to 5dp on focus with animation
- shapes.small=24dp rounded, shapes.medium=36dp rounded, shapes.large=CircleShape
- focusable=true enables focus outline

**Source**: experiments/ui/007-glimmer-depth-effects

---

## Custom Theme via Colors.copy() + Typography.copy()

**When to use**: When customizing Glimmer theme colors or typography for a specific screen
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text

// Create custom theme values
val customColors = GlimmerTheme.colors.copy(
    primary = Color(0xFFFF9800),    // Override just primary
    positive = Color(0xFF00BCD4),   // Override just positive
)
val customTypography = GlimmerTheme.typography.copy(
    titleLarge = GlimmerTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Black
    ),
)

// Apply via nested GlimmerTheme - only affects subtree
GlimmerTheme(colors = customColors, typography = customTypography) {
    Text("This uses custom theme", color = GlimmerTheme.colors.primary) // Orange
}
// Outside the nested theme, original colors still apply
```

**Gotchas**:
- Nested GlimmerTheme only affects its subtree children
- `Colors.copy()` preserves all unspecified colors (only overrides what you pass)
- `Typography.copy()` preserves all unspecified styles
- Individual TextStyle can be further customized with `.copy(fontWeight = ...)`
- Must access theme values via `GlimmerTheme.colors`/`GlimmerTheme.typography` inside the theme scope

**Source**: experiments/ui/013-glimmer-typography-colors

---

## Glimmer Typography Scale Reference

**When to use**: When choosing the right typography style for transparent display readability
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text

// Title styles (Weight 750, bold) - for headings and emphasis
Text("Large Heading", style = GlimmerTheme.typography.titleLarge)   // 30sp
Text("Medium Heading", style = GlimmerTheme.typography.titleMedium) // 24sp
Text("Small Heading", style = GlimmerTheme.typography.titleSmall)   // 20sp

// Body styles (Weight 520, regular) - for content
Text("Large Body", style = GlimmerTheme.typography.bodyLarge)       // 30sp
Text("Medium Body", style = GlimmerTheme.typography.bodyMedium)     // 24sp
Text("Small Body (default)", style = GlimmerTheme.typography.bodySmall) // 20sp

// Caption (Weight 650, semi-bold) - for labels and metadata
Text("Caption Text", style = GlimmerTheme.typography.caption)       // 18sp
```

**Gotchas**:
- bodySmall is the default text style in GlimmerTheme
- All styles use Google Sans Flex font family
- Caption uses 650 weight (semi-bold), heavier than body (520) but lighter than title (750)
- On transparent display, titleLarge/bodyLarge (30sp) have best distant readability
- caption (18sp) should only be used for supplementary information

**Source**: experiments/ui/013-glimmer-typography-colors

---
