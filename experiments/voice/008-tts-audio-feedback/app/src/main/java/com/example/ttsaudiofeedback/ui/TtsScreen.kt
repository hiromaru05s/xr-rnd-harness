package com.example.ttsaudiofeedback.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items

@Composable
fun TtsScreen(
    ttsState: String,
    feedbackMode: String,
    onSpeakWelcome: () -> Unit,
    onSpeakStatus: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            ) { Text("Mode: $feedbackMode") }

            Card(
                title = { Text("TTS状態") },
                action = {
                    Button(
                        onClick = onStop,
                        buttonSize = ButtonSize.Medium,
                        color = GlimmerTheme.colors.negative,
                    ) { Text("停止") }
                },
            ) {
                Text(text = ttsState, style = GlimmerTheme.typography.bodySmall)
            }

            data class TtsAction(val label: String, val action: () -> Unit)
            val actions = listOf(
                TtsAction("ウェルカムメッセージ", onSpeakWelcome),
                TtsAction("ステータス読み上げ", onSpeakStatus),
            )

            VerticalList(horizontalAlignment = Alignment.CenterHorizontally) {
                items(actions) { item ->
                    ListItem(
                        onClick = item.action,
                        leadingIcon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null) },
                    ) { Text(item.label) }
                }
            }
        }
    }
}
