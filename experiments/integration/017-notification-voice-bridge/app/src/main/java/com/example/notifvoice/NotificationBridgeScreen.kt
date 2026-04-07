package com.example.notifvoice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items
import androidx.xr.glimmer.onIndirectPointerGesture

@Composable
fun NotificationBridgeScreen(
    bridgeState: BridgeState,
    ttsStatus: TtsStatus,
    onSwipeForward: () -> Unit,
    onSwipeBackward: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .onIndirectPointerGesture(
                onSwipeForward = onSwipeForward,
                onSwipeBackward = onSwipeBackward,
                onClick = onClick)
            .focusTarget(),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {

            val statusIcon = when (ttsStatus) {
                TtsStatus.Speaking -> Icons.Default.VolumeUp
                else -> Icons.Default.Notifications
            }
            TitleChip(leadingIcon = { Icon(imageVector = statusIcon, contentDescription = null) }) {
                Text(getStatusText(bridgeState, ttsStatus))
            }

            when (bridgeState) {
                is BridgeState.Initializing -> {
                    Card(title = { Text("Initializing") }) { Text("Setting up voice bridge...") }
                }
                is BridgeState.WaitingForNotifications -> {
                    Card(title = { Text("Waiting") },
                        leadingIcon = { Icon(imageVector = Icons.Default.NotificationsOff, contentDescription = null) }
                    ) { Text("No notifications yet") }
                }
                is BridgeState.ShowingNotification -> {
                    val notif = bridgeState.current
                    Card(title = { Text(notif.appName) },
                        subtitle = { Text((bridgeState.currentIndex + 1).toString() + "/" + bridgeState.queueSize.toString()) },
                        action = {
                            Button(onClick = onClick, buttonSize = ButtonSize.Medium,
                                color = GlimmerTheme.colors.negative
                            ) { Text("Dismiss") }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(notif.title, style = GlimmerTheme.typography.titleSmall)
                            Text(notif.text)
                        }
                    }
                }
                is BridgeState.Reading -> {
                    val notif = bridgeState.current
                    Card(title = { Text(notif.appName) },
                        leadingIcon = { Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null) }
                    ) { Text(notif.title + ": " + notif.text) }
                }
                is BridgeState.Error -> {
                    Card(title = { Text("Error") }) { Text(bridgeState.message) }
                }
            }
        }
    }
}

private fun getStatusText(state: BridgeState, tts: TtsStatus): String {
    return when {
        tts == TtsStatus.Speaking -> "Reading aloud..."
        state is BridgeState.ShowingNotification -> "Swipe to navigate"
        state is BridgeState.WaitingForNotifications -> "Waiting"
        state is BridgeState.Error -> "Error"
        else -> "Notification Bridge"
    }
}
