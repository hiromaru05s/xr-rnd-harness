package com.example.voicetouchpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.onIndirectPointerGesture

enum class TtsStatus { Idle, Speaking, Completed, Error }

@Composable
fun VoiceTouchpadScreen(
    cards: List<Pair<String, String>>,
    currentIndex: Int,
    areVisualsOn: Boolean,
    isVisualUiSupported: Boolean,
    ttsStatus: TtsStatus,
    onSwipeForward: () -> Unit,
    onSwipeBackward: () -> Unit,
    onClick: () -> Unit,
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
                onClick = onClick,
            )
            .focusable(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val ttsIcon = when (ttsStatus) {
                TtsStatus.Speaking -> Icons.Default.Star
                TtsStatus.Completed -> Icons.Default.Check
                TtsStatus.Error -> Icons.Default.Warning
                TtsStatus.Idle -> Icons.Default.Info
            }
            val modeLabel = if (areVisualsOn) "Visual+Voice" else "Voice Only"
            val pageLabel = (currentIndex + 1).toString() + "/" + cards.size.toString() + " " + modeLabel
            TitleChip(leadingIcon = { Icon(imageVector = ttsIcon, contentDescription = null) }) {
                Text(pageLabel)
            }

            if (isVisualUiSupported && areVisualsOn) {
                val (title, description) = cards[currentIndex]
                Card(
                    title = { Text(title) },
                    subtitle = { Text("Swipe to navigate") },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = description,
                            style = GlimmerTheme.typography.bodySmall,
                        )
                        val ttsLabel = when (ttsStatus) {
                            TtsStatus.Speaking -> "Speaking..."
                            TtsStatus.Completed -> "Done"
                            TtsStatus.Error -> "TTS Error"
                            TtsStatus.Idle -> "Tap to speak"
                        }
                        Text(
                            text = "TTS: " + ttsLabel,
                            style = GlimmerTheme.typography.caption,
                            color = when (ttsStatus) {
                                TtsStatus.Speaking -> GlimmerTheme.colors.primary
                                TtsStatus.Completed -> GlimmerTheme.colors.positive
                                TtsStatus.Error -> GlimmerTheme.colors.negative
                                TtsStatus.Idle -> GlimmerTheme.colors.outline
                            },
                        )
                    }
                }
            } else {
                Card(title = { Text("Audio Only Mode") }) {
                    Text(
                        text = "Display off. Use touchpad to navigate. Voice reads each card.",
                        style = GlimmerTheme.typography.bodySmall,
                        color = GlimmerTheme.colors.primary,
                    )
                }
            }
        }
    }
}
