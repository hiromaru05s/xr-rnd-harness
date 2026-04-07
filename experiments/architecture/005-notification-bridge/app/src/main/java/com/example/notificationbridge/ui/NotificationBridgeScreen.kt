package com.example.notificationbridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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

/**
 * 通知ブリッジのUI画面。
 * 通知送信ボタンと最新結果を最小限のUIで表示。
 */
@Composable
fun NotificationBridgeScreen(
    notificationCount: Int,
    lastResult: String,
    onSendStandard: () -> Unit,
    onSendMessaging: () -> Unit,
    onCancelAll: () -> Unit,
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
            // ステータス表示
            TitleChip(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                }
            ) {
                Text("通知: $notificationCount 件送信")
            }

            // 結果カード
            Card(
                title = { Text("最新結果") },
                action = {
                    Button(
                        onClick = onCancelAll,
                        buttonSize = ButtonSize.Medium,
                        color = GlimmerTheme.colors.negative,
                    ) { Text("全消去") }
                },
            ) {
                Text(
                    text = lastResult,
                    style = GlimmerTheme.typography.bodySmall,
                )
            }

            // アクションリスト（3アイテム以下）
            data class NotificationAction(val label: String, val action: () -> Unit)
            val actions = listOf(
                NotificationAction("標準通知を送信", onSendStandard),
                NotificationAction("会話通知を送信", onSendMessaging),
            )

            VerticalList(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(actions) { item ->
                    ListItem(
                        onClick = item.action,
                    ) {
                        Text(item.label)
                    }
                }
            }
        }
    }
}
