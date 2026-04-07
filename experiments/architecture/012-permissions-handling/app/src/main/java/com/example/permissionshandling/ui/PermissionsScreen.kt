package com.example.permissionshandling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
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
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items

enum class PermissionItemState {
    NOT_REQUESTED, REQUESTING, GRANTED, DENIED
}

@Composable
fun PermissionsScreen(
    cameraState: PermissionItemState,
    audioState: PermissionItemState,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allGranted = cameraState == PermissionItemState.GRANTED && audioState == PermissionItemState.GRANTED
    val anyDenied = cameraState == PermissionItemState.DENIED || audioState == PermissionItemState.DENIED

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black).padding(24.dp, 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val statusIcon = when {
                allGranted -> Icons.Default.Check
                anyDenied -> Icons.Default.Warning
                else -> Icons.Default.Settings
            }
            val statusLabel = when {
                allGranted -> "All Granted"
                anyDenied -> "Some Denied"
                else -> "Permissions"
            }
            TitleChip(leadingIcon = { Icon(imageVector = statusIcon, contentDescription = null) }) {
                Text(statusLabel)
            }

            Card(
                title = { Text("Hardware Access") },
                subtitle = { Text("ProjectedPermissionsResultContract") },
                action = {
                    if (!allGranted) {
                        Button(onClick = onRequestPermissions) {
                            Text(if (anyDenied) "Retry" else "Request")
                        }
                    }
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CAMERA: " + stateLabel(cameraState),
                        style = GlimmerTheme.typography.bodySmall,
                        color = stateColor(cameraState),
                    )
                    Text(
                        text = "RECORD_AUDIO: " + stateLabel(audioState),
                        style = GlimmerTheme.typography.bodySmall,
                        color = stateColor(audioState),
                    )
                }
            }

            if (anyDenied) {
                Card(title = { Text("Retry Available") }) {
                    Text(
                        text = "TTS fallback active. Tap Retry to re-request permissions.",
                        style = GlimmerTheme.typography.bodySmall,
                        color = GlimmerTheme.colors.primary,
                    )
                }
            }
        }
    }
}

private fun stateLabel(state: PermissionItemState): String = when (state) {
    PermissionItemState.NOT_REQUESTED -> "Not Requested"
    PermissionItemState.REQUESTING -> "Requesting..."
    PermissionItemState.GRANTED -> "Granted"
    PermissionItemState.DENIED -> "Denied"
}

@Composable
private fun stateColor(state: PermissionItemState): Color = when (state) {
    PermissionItemState.GRANTED -> GlimmerTheme.colors.positive
    PermissionItemState.DENIED -> GlimmerTheme.colors.negative
    PermissionItemState.REQUESTING -> GlimmerTheme.colors.primary
    PermissionItemState.NOT_REQUESTED -> GlimmerTheme.colors.outline
}
