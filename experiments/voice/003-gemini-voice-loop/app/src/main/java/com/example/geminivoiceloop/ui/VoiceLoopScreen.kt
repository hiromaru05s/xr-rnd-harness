package com.example.geminivoiceloop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
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
import com.example.geminivoiceloop.ConversationState

/**
 * Voice conversation loop UI for AI glasses.
 *
 * Minimal glance-able display showing:
 * - Current conversation state (Listening/Thinking/Speaking)
 * - Last transcript from user
 * - Start/Stop conversation button
 *
 * Designed for transparent display: black background, minimal content.
 */
@Composable
fun VoiceLoopScreen(
    conversationState: ConversationState,
    lastTranscript: String,
    lastResponse: String,
    errorMessage: String?,
    onStartConversation: () -> Unit,
    onStopConversation: () -> Unit,
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
            // State indicator
            TitleChip(
                leadingIcon = {
                    Icon(
                        imageVector = when (conversationState) {
                            ConversationState.LISTENING -> Icons.Default.Mic
                            ConversationState.ERROR -> Icons.Default.Info
                            else -> Icons.Default.Star
                        },
                        contentDescription = null,
                    )
                }
            ) {
                Text(conversationState.displayLabel)
            }

            // Main card: transcript or prompt
            Card(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        when (conversationState) {
                            ConversationState.IDLE -> "Gemini Voice"
                            ConversationState.CONNECTING -> "Connecting"
                            ConversationState.LISTENING -> "You said"
                            ConversationState.THINKING -> "Processing"
                            ConversationState.SPEAKING -> "Gemini"
                            ConversationState.ERROR -> "Error"
                        }
                    )
                },
                action = {
                    when (conversationState) {
                        ConversationState.IDLE, ConversationState.ERROR -> {
                            Button(
                                onClick = onStartConversation,
                                buttonSize = ButtonSize.Medium,
                                leadingIcon = { Icon(Icons.Default.Mic, null) },
                            ) { Text("Start") }
                        }
                        else -> {
                            Button(
                                onClick = onStopConversation,
                                buttonSize = ButtonSize.Medium,
                                leadingIcon = { Icon(Icons.Default.MicOff, null) },
                                color = GlimmerTheme.colors.negative,
                            ) { Text("Stop") }
                        }
                    }
                }
            ) {
                Text(
                    when {
                        errorMessage != null -> errorMessage
                        lastTranscript.isNotEmpty() -> lastTranscript
                        conversationState == ConversationState.IDLE ->
                            "Tap Start to begin voice conversation with Gemini."
                        conversationState == ConversationState.CONNECTING ->
                            "Establishing connection..."
                        else -> "..."
                    }
                )
            }
        }
    }
}
