package com.example.glimmerdepth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.SurfaceDefaults
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.surface

@Composable
fun DepthEffectsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TitleChip(
                leadingIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) }
            ) { Text("DepthEffect Demo") }

            // Card 1: Default surface
            Card(
                title = { Text("Default Surface") },
                subtitle = { Text("Standard border 2dp") },
            ) {
                Text(
                    text = "Default GlimmerTheme.colors.surface color with auto-applied border.",
                    style = GlimmerTheme.typography.bodySmall,
                )
            }

            // Card 2: Custom color + custom border
            Card(
                title = { Text("Custom Surface") },
                subtitle = { Text("Primary color border") },
                leadingIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                color = GlimmerTheme.colors.surface,
                border = SurfaceDefaults.border(width = 3.dp, color = GlimmerTheme.colors.primary),
            ) {
                Text(
                    text = "SurfaceDefaults.border(width, color) for custom borders.",
                    style = GlimmerTheme.typography.bodySmall,
                    color = GlimmerTheme.colors.primary,
                )
            }

            // Box with surface modifier
            Box(
                modifier = Modifier.fillMaxWidth()
                    .surface(
                        focusable = true,
                        shape = GlimmerTheme.shapes.small,
                        color = GlimmerTheme.colors.surface,
                        border = BorderStroke(2.dp, GlimmerTheme.colors.positive),
                    )
                    .padding(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Modifier.surface()",
                        style = GlimmerTheme.typography.titleSmall,
                        color = GlimmerTheme.colors.positive,
                    )
                    Text(
                        text = "focusable=true enables focus outline. shapes.small = RoundedCornerShape(24.dp). Focus expands border to 5dp with animation.",
                        style = GlimmerTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
