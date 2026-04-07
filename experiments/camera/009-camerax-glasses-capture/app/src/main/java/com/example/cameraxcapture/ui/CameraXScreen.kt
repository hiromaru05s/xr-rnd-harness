package com.example.cameraxcapture.ui
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

sealed class CaptureState {
    data object Initializing : CaptureState()
    data object RequestingPermission : CaptureState()
    data object PermissionGranted : CaptureState()
    data object PermissionDenied : CaptureState()
    data object BindingCamera : CaptureState()
    data class Ready(val resolution: String, val cameraSelector: String) : CaptureState()
    data class Error(val message: String) : CaptureState()
}

@Composable
fun CameraXScreen(
    captureState: CaptureState,
    onRetryPermission: () -> Unit,
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
            val statusText = when (captureState) {
                is CaptureState.Initializing -> "Initializing..."
                is CaptureState.RequestingPermission -> "Requesting Permission"
                is CaptureState.PermissionGranted -> "Permission OK"
                is CaptureState.PermissionDenied -> "Permission Denied"
                is CaptureState.BindingCamera -> "Binding Camera..."
                is CaptureState.Ready -> "Camera Ready"
                is CaptureState.Error -> "Error"
            }
            val statusIcon = when (captureState) {
                is CaptureState.Ready -> Icons.Default.Check
                is CaptureState.Error, is CaptureState.PermissionDenied -> Icons.Default.Warning
                else -> Icons.Default.Info
            }
            TitleChip(leadingIcon = { Icon(imageVector = statusIcon, contentDescription = null) }) { Text(statusText) }

            when (captureState) {
                is CaptureState.Ready -> {
                    Card(title = { Text("Glasses Camera") }, subtitle = { Text("ProjectedDeviceContext") }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "CameraX bound via projected context", style = GlimmerTheme.typography.bodySmall, color = GlimmerTheme.colors.positive)
                            Text(text = "Resolution: " + captureState.resolution, style = GlimmerTheme.typography.bodySmall)
                            Text(text = "Selector: " + captureState.cameraSelector, style = GlimmerTheme.typography.bodySmall)
                        }
                    }
                }
                is CaptureState.PermissionDenied -> {
                    Card(title = { Text("Permission Required") }, action = { Button(onClick = onRetryPermission) { Text("Retry") } }) {
                        Text(text = "Camera access is needed to use glasses camera.", style = GlimmerTheme.typography.bodySmall, color = GlimmerTheme.colors.negative)
                    }
                }
                is CaptureState.Error -> {
                    Card(title = { Text("Camera Error") }) {
                        Text(text = captureState.message, style = GlimmerTheme.typography.bodySmall, color = GlimmerTheme.colors.negative)
                    }
                }
                else -> {
                    Card(title = { Text("Setting Up Camera") }) {
                        Text(text = "Please wait...", style = GlimmerTheme.typography.bodySmall, color = GlimmerTheme.colors.primary)
                    }
                }
            }
        }
    }
}
