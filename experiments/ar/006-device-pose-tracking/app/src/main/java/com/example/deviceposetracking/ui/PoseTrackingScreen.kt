package com.example.deviceposetracking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
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
import androidx.xr.runtime.math.Pose

/**
 * デバイスポーズ情報をリアルタイムで表示するスクリーン。
 *
 * TitleChipでトラッキング状態、Cardでposition(x,y,z)とrotation(x,y,z,w)を表示。
 * AIグラスのFOV制約内でグランス可能な最小限の情報量。
 */
@Composable
fun PoseTrackingScreen(
    currentPose: Pose?,
    trackingState: String,
    sessionStatus: String,
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
            // トラッキング状態
            TitleChip(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Place, contentDescription = null)
                }
            ) {
                Text(trackingState)
            }

            // ポジション情報カード
            Card(
                title = { Text("デバイスポーズ") },
                subtitle = { Text(sessionStatus) },
            ) {
                if (currentPose != null) {
                    val t = currentPose.translation
                    val r = currentPose.rotation
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "位置: (%.2f, %.2f, %.2f)".format(t.x, t.y, t.z),
                            style = GlimmerTheme.typography.bodySmall,
                            color = GlimmerTheme.colors.positive,
                        )
                        Text(
                            text = "回転: (%.2f, %.2f, %.2f, %.2f)".format(r.x, r.y, r.z, r.w),
                            style = GlimmerTheme.typography.bodySmall,
                            color = GlimmerTheme.colors.primary,
                        )
                    }
                } else {
                    Text(
                        text = "ポーズデータ待機中...",
                        style = GlimmerTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
