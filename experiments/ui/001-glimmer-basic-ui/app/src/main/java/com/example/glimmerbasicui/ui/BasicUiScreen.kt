package com.example.glimmerbasicui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items

/**
 * Glimmer基本UIコンポーネントのショーケース画面。
 *
 * 黒背景（透過ディスプレイでは透明）上に以下を配置:
 * - TitleChip: 選択状態をステータス表示
 * - Card + Button (Medium/Large + アイコン付き): アクション操作
 * - VerticalList + ListItem x3: タッチパッドフォーカスナビゲーション確認
 *
 * GlimmerTheme のフォーカスシステムがアウトラインベースのハイライトを自動管理する。
 *
 * [FB対応] テキストの中央揃えを徹底:
 * - VerticalList に horizontalAlignment = Alignment.CenterHorizontally を明示
 * - Column/Box の中央揃え設定を再確認
 * - fillMaxWidth で均等幅レイアウトを確保
 */
@Composable
fun BasicUiScreen(modifier: Modifier = Modifier) {
    var selectedLabel by remember { mutableStateOf("選択なし") }

    // 黒背景 = 加算光方式で透過ディスプレイ上では透明になる
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
            // ステータス表示: 選択中アイテムをTitleChipで示す
            TitleChip(
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                    )
                }
            ) {
                Text(selectedLabel)
            }

            // Card + Button (Medium確認 + Large+アイコン確認)
            Card(
                modifier = Modifier.fillMaxWidth(),
                title = { Text("アクション") },
                action = {
                    // Mediumサイズ Button
                    Button(
                        onClick = { selectedLabel = "確認済み" },
                        buttonSize = ButtonSize.Medium,
                    ) {
                        Text("確認")
                    }
                }
            ) {
                // Largeサイズ + 先頭アイコン付き Button
                Button(
                    onClick = { selectedLabel = "実行中" },
                    buttonSize = ButtonSize.Large,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                    }
                ) {
                    Text("実行")
                }
            }

            // [FB対応] VerticalList に horizontalAlignment を明示して中央揃え
            // タッチパッドの前後スワイプでフォーカスが移動し、アウトラインが変化する
            VerticalList(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(menuItems) { item ->
                    ListItem(
                        onClick = { selectedLabel = item.label },
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                            )
                        },
                        supportingLabel = { Text("タップで選択") },
                    ) {
                        Text(item.label)
                    }
                }
            }
        }
    }
}

private data class MenuItem(val label: String, val icon: ImageVector)

private val menuItems = listOf(
    MenuItem("通知", Icons.Default.Notifications),
    MenuItem("設定", Icons.Default.Settings),
    MenuItem("ヘルプ", Icons.Default.Info),
)
