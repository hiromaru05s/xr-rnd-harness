package com.example.hostdevicecontext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip

data class ContextInfo(
    val isGlassesContext: Boolean = false,
    val deviceName: String = "unknown",
    val hostContextAvailable: Boolean = false,
    val isHostProjected: Boolean = false,
    val errorMessage: String = "",
)

@Composable
fun HostContextScreen(
    info: ContextInfo,
    onVibratePhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black).padding(24.dp, 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val icon = if (info.hostContextAvailable) Icons.Default.Check else Icons.Default.Warning
            TitleChip(leadingIcon = { Icon(imageVector = icon, contentDescription = null) }) {
                Text("Host Context")
            }

            Card(title = { Text("Context Detection") }, subtitle = { Text("ProjectedContext API") }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "isProjectedDeviceContext: " + info.isGlassesContext.toString(),
                        style = GlimmerTheme.typography.bodySmall,
                        color = if (info.isGlassesContext) GlimmerTheme.colors.positive else GlimmerTheme.colors.outline,
                    )
                    Text(text = "deviceName: " + info.deviceName, style = GlimmerTheme.typography.bodySmall)
                    Text(
                        text = "hostContext: " + if (info.hostContextAvailable) "Available" else "N/A",
                        style = GlimmerTheme.typography.bodySmall,
                        color = if (info.hostContextAvailable) GlimmerTheme.colors.positive else GlimmerTheme.colors.negative,
                    )
                    if (info.hostContextAvailable) {
                        Text(text = "host isProjected: " + info.isHostProjected.toString(), style = GlimmerTheme.typography.caption)
                    }
                    if (info.errorMessage.isNotEmpty()) {
                        Text(text = "Error: " + info.errorMessage, style = GlimmerTheme.typography.caption, color = GlimmerTheme.colors.negative)
                    }
                }
            }

            if (info.hostContextAvailable) {
                Card(
                    title = { Text("Phone Hardware") },
                    action = { Button(onClick = onVibratePhone) { Text("Vibrate") } },
                ) {
                    Text(
                        text = "Access phone vibrator from glasses via HostDeviceContext",
                        style = GlimmerTheme.typography.bodySmall,
                        color = GlimmerTheme.colors.primary,
                    )
                }
            }
        }
    }
}
