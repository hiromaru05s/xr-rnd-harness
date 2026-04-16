package com.example.glimmerbasicui.ui

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

/**
 * Showcase screen for basic Glimmer UI components.
 *
 * Renders on black background (transparent on see-through display):
 * - TitleChip: shows current selection status
 * - Card + Button (Medium/Large + icon): action controls
 * - VerticalList + ListItem x3: touchpad focus navigation demo
 *
 * GlimmerTheme's focus system auto-manages outline-based highlights.
 *
 * [FB fix #3] All UI text changed from Japanese to English to resolve
 * character encoding / mojibake issue on emulator display.
 *
 * [FB fix #2] Text centering:
 * - VerticalList: horizontalAlignment = Alignment.CenterHorizontally
 * - Column/Box center alignment verified
 * - fillMaxWidth for uniform layout
 */
@Composable
fun BasicUiScreen(modifier: Modifier = Modifier) {
    var selectedLabel by remember { mutableStateOf("None") }

    // Black background = transparent on see-through display (additive light)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Status display: show selected item via TitleChip
            TitleChip(
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                    )
                }
            ) {
                Text(selectedLabel)
            }

            // Card + Button (Medium + Large with icon)
            Card(
                modifier = Modifier.fillMaxWidth(),
                title = { Text("Actions") },
                action = {
                    // Medium-size Button
                    Button(
                        onClick = { selectedLabel = "Confirmed" },
                        buttonSize = ButtonSize.Medium,
                    ) {
                        Text("OK")
                    }
                }
            ) {
                // Large-size Button with leading icon
                Button(
                    onClick = { selectedLabel = "Running" },
                    buttonSize = ButtonSize.Large,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    }
                ) {
                    Text("Run")
                }
            }

            // [FB fix #2] VerticalList with explicit horizontalAlignment for centering
            // Touchpad forward/backward swipe moves focus with outline highlight
            VerticalList(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(menuItems) { item ->
                    ListItem(
                        onClick = { selectedLabel = item.label },
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                            )
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

private data class MenuItem(val label: String, val icon: ImageVector)

private val menuItems = listOf(
    MenuItem("Alerts", Icons.Default.Notifications),
    MenuItem("Settings", Icons.Default.Settings),
    MenuItem("Help", Icons.Default.Info),
)
