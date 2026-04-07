package com.example.geminifunctioncall.ui

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
import androidx.compose.material.icons.filled.Star
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

enum class AgentState { Disconnected, Connecting, Connected, Error }

@Composable
fun FunctionCallScreen(
    agentState: AgentState,
    items: List<String>,
    onConnect: () -> Unit,
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
            val statusIcon = when (agentState) {
                AgentState.Connected -> Icons.Default.Check
                AgentState.Error -> Icons.Default.Warning
                AgentState.Connecting -> Icons.Default.Star
                AgentState.Disconnected -> Icons.Default.Info
            }
            val statusLabel = when (agentState) {
                AgentState.Connected -> "Agent Active"
                AgentState.Connecting -> "Connecting..."
                AgentState.Error -> "Error"
                AgentState.Disconnected -> "Disconnected"
            }
            TitleChip(leadingIcon = { Icon(imageVector = statusIcon, contentDescription = null) }) {
                Text(statusLabel)
            }

            if (agentState == AgentState.Disconnected) {
                Card(
                    title = { Text("Gemini Function Calling") },
                    action = { Button(onClick = onConnect) { Text("Connect") } },
                ) {
                    Text(
                        text = "Voice commands: add/remove/list items",
                        style = GlimmerTheme.typography.bodySmall,
                    )
                }
            } else if (agentState == AgentState.Connected) {
                Card(title = { Text("Shopping List") }, subtitle = { Text(items.size.toString() + " items") }) {
                    if (items.isEmpty()) {
                        Text(
                            text = "Say: Add milk, eggs, bread",
                            style = GlimmerTheme.typography.bodySmall,
                            color = GlimmerTheme.colors.primary,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items.take(3).forEach { item ->
                                Text(
                                    text = "- " + item,
                                    style = GlimmerTheme.typography.bodySmall,
                                    color = GlimmerTheme.colors.positive,
                                )
                            }
                            if (items.size > 3) {
                                Text(
                                    text = "+" + (items.size - 3) + " more",
                                    style = GlimmerTheme.typography.caption,
                                )
                            }
                        }
                    }
                }
            } else if (agentState == AgentState.Error) {
                Card(title = { Text("Connection Error") }) {
                    Text(
                        text = "Failed to connect to Gemini",
                        style = GlimmerTheme.typography.bodySmall,
                        color = GlimmerTheme.colors.negative,
                    )
                    Button(onClick = onConnect) { Text("Retry") }
                }
            } else {
                Card(title = { Text("Connecting...") }) {
                    Text(text = "Please wait", style = GlimmerTheme.typography.bodySmall, color = GlimmerTheme.colors.primary)
                }
            }
        }
    }
}
