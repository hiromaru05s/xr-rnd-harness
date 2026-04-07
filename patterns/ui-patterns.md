# Glimmer UIパターン集

> このファイルはAIがコンテキストとして読み込み、vibe codingの参照にする。
> コードスニペットはコピペで動くレベルの完全性を維持すること。

---

## Glimmer基本コンポーネント配置

**いつ使う**: AIグラス向けに基本的なGlimmer UIコンポーネントを配置する画面を作るとき
**前提**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`, `implementation(platform("androidx.compose:compose-bom:2025.01.00"))`, `implementation("androidx.compose.material:material-icons-core")`

```kotlin
// === Activity: GlimmerThemeでラップする最小構成 ===
package com.example.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.xr.glimmer.GlimmerTheme

class GlassesMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // GlimmerThemeでラップするだけでフォーカスシステムが有効化される
            GlimmerTheme {
                MyScreen()
            }
        }
    }
}
```

```kotlin
// === Screen: 黒背景 + 基本コンポーネント配置 ===
package com.example.myapp.ui

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

@Composable
fun MyScreen(modifier: Modifier = Modifier) {
    var selectedLabel by remember { mutableStateOf("選択なし") }

    // 黒背景 = 加算光方式で透明。UIコンテンツのみ視認される
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
            // TitleChip: ステータスバー的な表示
            TitleChip(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null)
                }
            ) {
                Text(selectedLabel)
            }

            // Card: title/action/contentの3スロット構成
            Card(
                title = { Text("アクション") },
                action = {
                    // Mediumサイズ Button
                    Button(
                        onClick = { selectedLabel = "確認済み" },
                        buttonSize = ButtonSize.Medium,
                    ) { Text("確認") }
                }
            ) {
                // Largeサイズ + アイコン付き Button
                Button(
                    onClick = { selectedLabel = "実行中" },
                    buttonSize = ButtonSize.Large,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    }
                ) { Text("実行") }
            }

            // VerticalList: 3アイテム以下（グラスのFOV制約）
            // DPAD_DOWN/UPでアウトラインフォーカスが自動移動（focusRequester不要）
            VerticalList {
                data class MenuItem(val label: String, val icon: ImageVector)
                val menuItems = listOf(
                    MenuItem("通知", Icons.Default.Notifications),
                    MenuItem("設定", Icons.Default.Settings),
                    MenuItem("ヘルプ", Icons.Default.Info),
                )
                items(menuItems) { item ->
                    ListItem(
                        onClick = { selectedLabel = item.label },
                        leadingIcon = {
                            Icon(imageVector = item.icon, contentDescription = null)
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
```

```xml
<!-- === AndroidManifest.xml: 2アクティビティ構成（公式推奨） === -->
<!-- 注意: GlassesMainActivityにLAUNCHERカテゴリを付けるのはNG -->
<application>
    <activity
        android:name=".MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity
        android:name=".GlassesMainActivity"
        android:exported="true"
        android:requiredDisplayCategory="xr_projected"
        android:label="My Glass App">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
        </intent-filter>
    </activity>
</application>

**ハマりポイント**:
- `LazyColumn` は使わない。必ず `VerticalList` + `items()` を使う
- 背景は必ず `Color.Black`（加算光方式で透明になる）
- `android:requiredDisplayCategory="xr_projected"` がないとグラスに投影されない
- `GlimmerTheme` でラップしないとフォーカスシステムが動かない
- リストは3アイテム以下（FOV 50-70度の制約）
- Glimmerの `Text`, `Button`, `Icon` 等はすべて `androidx.xr.glimmer` パッケージからimport（Compose Materialのものではない）

**出典**: experiments/ui/001-glimmer-basic-ui

---

## TitleChipによるステータス表示

**いつ使う**: 画面上部に現在の状態やモードを1行で表示したいとき
**前提**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip

// TitleChip: leadingIconスロット + contentラムダ
TitleChip(
    leadingIcon = {
        Icon(imageVector = Icons.Default.Star, contentDescription = null)
    }
) {
    Text("ステータステキスト")
}
```

**ハマりポイント**:
- TitleChipはタップ不可。表示専用コンポーネント
- アイコンは省略可能（leadingIcon引数を渡さなければよい）

**出典**: experiments/ui/001-glimmer-basic-ui

---

## Card + Button の組み合わせ

**いつ使う**: アクションカードを作りたいとき（タイトル付きのボタン群）
**前提**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text

// Card: title(上部ラベル), action(右上アクション), content(本体)
Card(
    title = { Text("カードタイトル") },
    action = {
        Button(
            onClick = { /* アクション */ },
            buttonSize = ButtonSize.Medium,
        ) { Text("ボタンM") }
    }
) {
    Button(
        onClick = { /* アクション */ },
        buttonSize = ButtonSize.Large,
        leadingIcon = {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        }
    ) { Text("ボタンL") }
}
```

**ハマりポイント**:
- `ButtonSize.Medium` と `ButtonSize.Large` のみ（Smallはグラスでは視認性が悪い）
- `leadingIcon` はLargeサイズとの組み合わせが見やすい

**出典**: experiments/ui/001-glimmer-basic-ui

## VerticalList 中央揃えパターン

**いつ使う**: VerticalListのアイテムを画面中央に揃えたいとき（デフォルトはAlignment.Start=左寄せ）
**前提**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.ui.Alignment
import androidx.xr.glimmer.ListItem
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.list.items

// horizontalAlignment を明示しないとデフォルトの Alignment.Start（左寄せ）になる
VerticalList(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    items(menuItems) { item ->
        ListItem(
            onClick = { /* アクション */ },
        ) {
            Text(item.label)
        }
    }
}
```

**ハマりポイント**:
- VerticalListのデフォルト `horizontalAlignment` は `Alignment.Start`。Column内でCenterHorizontallyを設定していても、VerticalListは独自のalignmentを使う
- 親のColumn/Boxが中央揃えでも、VerticalList内のアイテムは左寄せになるので注意
- 透過ディスプレイでは中央揃えが特に重要（視線の焦点が中央にあるため）

**出典**: experiments/ui/001-glimmer-basic-ui (人間フィードバック対応で発見)

---

## Surface/SurfaceDefaultsカスタマイズ

**いつ使う**: Card/ListItem/BoxのボーダーやSurface設定をカスタマイズしたいとき
**前提**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.SurfaceDefaults
import androidx.xr.glimmer.surface

// 1. Cardにカスタムボーダーとカラー
Card(
    title = { Text("Custom Card") },
    color = GlimmerTheme.colors.surface,
    border = SurfaceDefaults.border(
        width = 3.dp,
        color = GlimmerTheme.colors.primary,
    ),
) { /* content */ }

// 2. Modifier.surface()でフォーカス対応Boxを作成
Box(
    modifier = Modifier
        .surface(
            focusable = true,
            shape = GlimmerTheme.shapes.small, // RoundedCornerShape(24.dp)
            color = GlimmerTheme.colors.surface,
            border = BorderStroke(2.dp, GlimmerTheme.colors.positive),
        )
        .padding(16.dp), // .surface()の後に.padding()
) { /* content */ }
```

**ハマりポイント**:
- `.surface().padding()`の順序が重要。逆にするとボーダーがpaddingの内側に描画される
- SurfaceDefaults.border()はデフォルト2dp、フォーカス時5dpに拡大アニメーション
- shapes.small=24dp角丸、shapes.medium=36dp角丸、shapes.large=CircleShape
- focusable=trueでフォーカスアウトラインが有効化される

**出典**: experiments/ui/007-glimmer-depth-effects

---

## Custom Theme via Colors.copy() + Typography.copy()

**When to use**: When customizing Glimmer theme colors or typography for a specific screen
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text

// Create custom theme values
val customColors = GlimmerTheme.colors.copy(
    primary = Color(0xFFFF9800),    // Override just primary
    positive = Color(0xFF00BCD4),   // Override just positive
)
val customTypography = GlimmerTheme.typography.copy(
    titleLarge = GlimmerTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Black
    ),
)

// Apply via nested GlimmerTheme - only affects subtree
GlimmerTheme(colors = customColors, typography = customTypography) {
    Text("This uses custom theme", color = GlimmerTheme.colors.primary) // Orange
}
// Outside the nested theme, original colors still apply
```

**Gotchas**:
- Nested GlimmerTheme only affects its subtree children
- `Colors.copy()` preserves all unspecified colors (only overrides what you pass)
- `Typography.copy()` preserves all unspecified styles
- Individual TextStyle can be further customized with `.copy(fontWeight = ...)`
- Must access theme values via `GlimmerTheme.colors`/`GlimmerTheme.typography` inside the theme scope

**Source**: experiments/ui/013-glimmer-typography-colors

---

## Glimmer Typography Scale Reference

**When to use**: When choosing the right typography style for transparent display readability
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text

// Title styles (Weight 750, bold) - for headings and emphasis
Text("Large Heading", style = GlimmerTheme.typography.titleLarge)   // 30sp
Text("Medium Heading", style = GlimmerTheme.typography.titleMedium) // 24sp
Text("Small Heading", style = GlimmerTheme.typography.titleSmall)   // 20sp

// Body styles (Weight 520, regular) - for content
Text("Large Body", style = GlimmerTheme.typography.bodyLarge)       // 30sp
Text("Medium Body", style = GlimmerTheme.typography.bodyMedium)     // 24sp
Text("Small Body (default)", style = GlimmerTheme.typography.bodySmall) // 20sp

// Caption (Weight 650, semi-bold) - for labels and metadata
Text("Caption Text", style = GlimmerTheme.typography.caption)       // 18sp
```

**Gotchas**:
- bodySmall is the default text style in GlimmerTheme
- All styles use Google Sans Flex font family
- Caption uses 650 weight (semi-bold), heavier than body (520) but lighter than title (750)
- On transparent display, titleLarge/bodyLarge (30sp) have best distant readability
- caption (18sp) should only be used for supplementary information

**Source**: experiments/ui/013-glimmer-typography-colors

---
