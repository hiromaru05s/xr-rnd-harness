package com.example.glimmertypography.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.onIndirectPointerGesture

@Composable
fun TypographyScreen(
    currentPage: Int,
    onSwipeForward: () -> Unit,
    onSwipeBackward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp, 16.dp)
            .onIndirectPointerGesture(
                onSwipeForward = onSwipeForward,
                onSwipeBackward = onSwipeBackward,
            )
            .focusable(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val pageLabel = (currentPage + 1).toString() + "/3 " + when (currentPage) {
                0 -> "Typography"
                1 -> "Colors"
                2 -> "Custom Theme"
                else -> ""
            }
            TitleChip(leadingIcon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) }) {
                Text(pageLabel)
            }

            when (currentPage) {
                0 -> TypographyPage()
                1 -> ColorsPage()
                2 -> CustomThemePage()
            }
        }
    }
}

@Composable
private fun TypographyPage() {
    Card(title = { Text("Type Scale (7 styles)") }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "titleLarge", style = GlimmerTheme.typography.titleLarge)
            Text(text = "titleMedium", style = GlimmerTheme.typography.titleMedium)
            Text(text = "titleSmall", style = GlimmerTheme.typography.titleSmall)
        }
    }
    Card(title = { Text("Body + Caption") }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "bodyLarge", style = GlimmerTheme.typography.bodyLarge)
            Text(text = "bodyMedium", style = GlimmerTheme.typography.bodyMedium)
            Text(text = "bodySmall (default)", style = GlimmerTheme.typography.bodySmall)
            Text(text = "caption", style = GlimmerTheme.typography.caption)
        }
    }
}

@Composable
private fun ColorsPage() {
    Card(title = { Text("8-Color System") }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "primary (#9BBFFF)", color = GlimmerTheme.colors.primary, style = GlimmerTheme.typography.bodySmall)
            Text(text = "secondary (#4C88E9)", color = GlimmerTheme.colors.secondary, style = GlimmerTheme.typography.bodySmall)
            Text(text = "positive (#63FEA8)", color = GlimmerTheme.colors.positive, style = GlimmerTheme.typography.bodySmall)
            Text(text = "negative (#FFA7A0)", color = GlimmerTheme.colors.negative, style = GlimmerTheme.typography.bodySmall)
        }
    }
    Card(title = { Text("Surface Colors") }) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "background (Black=transparent)", style = GlimmerTheme.typography.bodySmall)
            Text(text = "surface (#262626)", style = GlimmerTheme.typography.bodySmall)
            Text(text = "outline (#606460)", color = GlimmerTheme.colors.outline, style = GlimmerTheme.typography.bodySmall)
            Text(text = "outlineVariant (#42434A)", color = GlimmerTheme.colors.outlineVariant, style = GlimmerTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CustomThemePage() {
    val customColors = GlimmerTheme.colors.copy(
        primary = Color(0xFFFF9800),
        positive = Color(0xFF00BCD4),
    )
    val customTypography = GlimmerTheme.typography.copy(
        titleLarge = GlimmerTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
    )
    GlimmerTheme(colors = customColors, typography = customTypography) {
        Card(title = { Text("Custom Theme") }, subtitle = { Text("Colors.copy() + Typography.copy()") }) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "Custom primary (Orange)", color = GlimmerTheme.colors.primary, style = GlimmerTheme.typography.bodySmall)
                Text(text = "Custom positive (Cyan)", color = GlimmerTheme.colors.positive, style = GlimmerTheme.typography.bodySmall)
                Text(text = "Custom titleLarge (Black weight)", style = GlimmerTheme.typography.titleLarge)
            }
        }
    }
}
