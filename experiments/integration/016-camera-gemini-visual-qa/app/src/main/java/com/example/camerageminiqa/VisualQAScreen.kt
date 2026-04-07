package com.example.camerageminiqa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip

@Composable
fun VisualQAScreen(
    appState: AppState,
    onCapture: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            TitleChip(leadingIcon = { StateIcon(appState) }) { Text(getStateLabel(appState)) }

            when (appState) {
                is AppState.Initializing -> { Card(title = { Text("Initializing") }) { Text("Setting up camera and AI...") } }
                is AppState.Ready -> {
                    Card(title = { Text("Ready") }, action = {
                        Button(onClick = onCapture, buttonSize = ButtonSize.Medium,
                            leadingIcon = { Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null) }
                        ) { Text("Capture") }
                    }) { Text("Press camera button or tap Capture") }
                }
                is AppState.Capturing -> { Card(title = { Text("Capturing") }) { Text("Taking photo...") } }
                is AppState.Analyzing -> { Card(title = { Text("Analyzing") }) { Text(appState.imageDescription) } }
                is AppState.Conversing -> {
                    Card(title = { Text("Conversation") }, action = {
                        Button(onClick = onCapture, buttonSize = ButtonSize.Medium,
                            leadingIcon = { Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null) }
                        ) { Text("New") }
                    }) {
                        val transcript = appState.lastTranscript
                        if (transcript.isNotEmpty()) { Text(transcript) } else { Text("Listening...") }
                    }
                }
                is AppState.Error -> {
                    Card(title = { Text("Error") }, action = {
                        Button(onClick = onRetry, buttonSize = ButtonSize.Medium,
                            color = GlimmerTheme.colors.negative,
                            leadingIcon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = null) }
                        ) { Text("Retry") }
                    }) { Text(appState.message) }
                }
            }
        }
    }
}

@Composable
private fun StateIcon(appState: AppState) {
    val icon = when (appState) {
        is AppState.Initializing -> Icons.Default.Refresh
        is AppState.Ready -> Icons.Default.CameraAlt
        is AppState.Capturing -> Icons.Default.PhotoCamera
        is AppState.Analyzing -> Icons.Default.Search
        is AppState.Conversing -> Icons.Default.Mic
        is AppState.Error -> Icons.Default.Error
    }
    Icon(imageVector = icon, contentDescription = null)
}

private fun getStateLabel(appState: AppState): String {
    return when (appState) {
        is AppState.Initializing -> "Initializing..."
        is AppState.Ready -> "Ready to capture"
        is AppState.Capturing -> "Capturing..."
        is AppState.Analyzing -> "AI Analyzing..."
        is AppState.Conversing -> "Conversing"
        is AppState.Error -> "Error"
    }
}
