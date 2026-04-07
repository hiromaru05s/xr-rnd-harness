package com.example.camerabuttoninput.ui

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
import androidx.compose.material.icons.filled.Refresh
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

/**
 * カメラボタン入力イベントの状態を表示するスクリーン。
 *
 * TitleChipでイベント受信状態、Cardで押下回数と最新イベント情報を表示。
 * AIグラスのFOV制約に配慮した最小限のUI。
 */
@Composable
fun CameraButtonScreen(
    cameraButtonCount: Int,
    isListening: Boolean,
    lastEventDescription: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // ステータス: イベント受信状態を表示
            TitleChip(
                leadingIcon = {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Check else Icons.Default.Info,
                        contentDescription = null,
                    )
                }
            ) {
                Text(if (isListening) "監視中" else "未接続")
            }

            // メインカード: カメラボタン押下回数と最新イベント
            Card(
                title = { Text("カメラボタン") },
                action = {
                    Button(
                        onClick = onReset,
                        buttonSize = ButtonSize.Medium,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                            )
                        },
                    ) {
                        Text("リセット")
                    }
                },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 押下回数をカスタムカラーで強調表示
                    Text(
                        text = "押下回数: $cameraButtonCount",
                        style = GlimmerTheme.typography.titleMedium,
                        color = if (cameraButtonCount > 0) {
                            GlimmerTheme.colors.positive
                        } else {
                            GlimmerTheme.colors.primary
                        },
                    )
                    // 最新イベント情報
                    Text(
                        text = lastEventDescription,
                        style = GlimmerTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
