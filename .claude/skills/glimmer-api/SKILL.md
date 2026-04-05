---
name: glimmer-api
description: Glimmer UIコンポーネントのAPIリファレンス。AIグラスのUI実装、Glimmerテーマ、色、タイポグラフィ、ボタン、カード、リスト、アイコン、Surface、タッチパッドジェスチャー等に関するタスクでは必ずこのスキルを使う。UIを書くときは常にこれを参照すること。
---

# Jetpack Compose Glimmer APIリファレンス

`androidx.xr.glimmer:glimmer:1.0.0-alpha08`

AIグラスの透過ディスプレイに最適化されたComposeベースのUIツールキット。
**Glimmerは他のXRライブラリ（Runtime, SceneCore等）に依存しない純粋なCompose UIライブラリ。**

依存: `androidx.compose.runtime`, `androidx.compose.ui`, `androidx.compose.foundation`, `androidx.annotation`のみ。

#### 1. Jetpack Compose Glimmer (`androidx.xr.glimmer:glimmer:1.0.0-alpha08`)

AIグラスの透過ディスプレイに最適化されたComposeベースのUIツールキット。
**Glimmerは他のXRライブラリ（Runtime, SceneCore等）に依存しない純粋なCompose UIライブラリ。**

依存: `androidx.compose.runtime`, `androidx.compose.ui`, `androidx.compose.foundation`, `androidx.annotation`のみ。

##### GlimmerTheme

```kotlin
@Composable
fun GlimmerTheme(
    colors: Colors = GlimmerTheme.colors,
    typography: Typography = GlimmerTheme.typography,
    componentSpacingValues: ComponentSpacingValues = GlimmerTheme.componentSpacingValues,
    content: @Composable () -> Unit,
)
```

GlimmerThemeのCompanionプロパティ（全て@Composable）:
- `GlimmerTheme.colors` / `GlimmerTheme.typography` / `GlimmerTheme.componentSpacingValues`
- `GlimmerTheme.shapes` / `GlimmerTheme.iconSizes` / `GlimmerTheme.depthEffectLevels`
- `GlimmerTheme.LocalGlimmerTheme` — テーマのCompositionLocal

##### Colors（8色）

| 名前 | 値 | 用途 |
|------|-----|------|
| `primary` | `#9BBFFF` | ブランドアクセント（ライトブルー） |
| `secondary` | `#4C88E9` | セカンダリアクセント（ミッドブルー） |
| `positive` | `#63FEA8` | 肯定・成功（グリーン） |
| `negative` | `#FFA7A0` | キャンセル・エラー（サーモンレッド） |
| `background` | `Color.Black` | 背景（透過ディスプレイでは透明になる） |
| `surface` | `#262626` | コンポーネント表面（ダークグレー） |
| `outline` | `#606460` | ボーダー（グレー） |
| `outlineVariant` | `#42434A` | 装飾ボーダー（ダークグレー） |

`copy()`メソッドで個別色のオーバーライド可能。

##### Typography（7スタイル、全てGoogle Sans Flex）

| スタイル | Weight | Size | Line Height |
|---------|--------|------|-------------|
| `titleLarge` | 750 | 30sp | 36sp |
| `titleMedium` | 750 | 24sp | 28sp |
| `titleSmall` | 750 | 20sp | 28sp |
| `bodyLarge` | 520 | 30sp | 36sp |
| `bodyMedium` | 520 | 24sp | 36sp |
| `bodySmall`（デフォルト） | 520 | 20sp | 28sp |
| `caption` | 650 | 18sp | 24sp |

コンストラクタに`defaultFontFamily`パラメータあり。`copy()`でスタイル個別オーバーライド可能。
`TypographyDefaults`シングルトンにデフォルト値定数あり。

##### Shapes

| レベル | Shape |
|--------|-------|
| `small` | `RoundedCornerShape(24.dp)` — カード用 |
| `medium` | `RoundedCornerShape(36.dp)` — デフォルト |
| `large` | `CircleShape` — ボタン用 |

##### ComponentSpacingValues

| レベル | 値 | 用途 |
|--------|-----|------|
| `extraSmall` | 6.dp | アイコン-テキスト間 |
| `small` | 8.dp | タイトル-サブタイトル間 |
| `medium` | 12.dp | カードのパディング等 |
| `large` | 16.dp | 構造的パディング |
| `extraLarge` | 20.dp | リストのアイテム間隔 |

##### IconSizes

| レベル | 値 |
|--------|-----|
| `small` | 32.dp — ボタン/チップアイコン |
| `medium` | 40.dp — デフォルト |
| `large` | 48.dp — カード/リストアイテムアイコン |

##### DepthEffectLevels（5段階）
各レベルは2つのShadowレイヤー（`DepthEffect(layer1: Shadow, layer2: Shadow)`）で構成。
level1が最小、level5が最大。フォーカス時に自動適用。

`SurfaceDepthEffect(depthEffect?, focusedDepthEffect?)` でフォーカス時の深度を個別設定可能。

##### CompositionLocals
- `LocalTextStyle: ProvidableCompositionLocal<TextStyle>` — デフォルトテキストスタイル
- `LocalIconSize: ProvidableCompositionLocal<Dp>` — デフォルトアイコンサイズ (40.dp)
- `LocalContentColor` — Surface経由で子要素に伝播するコンテンツ色

##### コンポーネント一覧と完全シグネチャ

**Text** (2オーバーロード: String / AnnotatedString)
標準Compose Textとほぼ同じシグネチャ。デフォルトスタイルは`LocalTextStyle.current`（GlimmerThemeで`bodySmall`に設定済み）。AnnotatedString版は`inlineContent`パラメータあり。

**Button**
```kotlin
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: ButtonSize = ButtonSize.Medium,  // Medium=48.dp, Large=72.dp
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = GlimmerTheme.shapes.large,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = ButtonDefaults.contentPadding(buttonSize),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
)
```
`ButtonSize`はインラインバリュークラス。`ButtonDefaults.contentPadding(buttonSize)`で適切なパディングを返す。
スペーシング: エッジ16.dp、アイコン-テキスト間6.dp。

**IconButton**
```kotlin
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlimmerTheme.shapes.large,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = PaddingValues(GlimmerTheme.componentSpacingValues.small),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
)
```
最小サイズ: 48.dp x 48.dp、デフォルトアイコンサイズ: small (32.dp)。level1のDepthEffect自動適用。

**Surface** (Modifier拡張、2オーバーロード: focusable / clickable)
```kotlin
// フォーカス可能なSurface
fun Modifier.surface(
    focusable: Boolean = true,
    shape: Shape = GlimmerTheme.shapes.medium,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    depthEffect: SurfaceDepthEffect? = null,
    border: BorderStroke? = SurfaceDefaults.border(),
    interactionSource: MutableInteractionSource? = null,
): Modifier

// クリック可能なSurface
fun Modifier.surface(
    enabled: Boolean = true,
    shape: Shape = GlimmerTheme.shapes.medium,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    depthEffect: SurfaceDepthEffect? = null,
    border: BorderStroke? = SurfaceDefaults.border(),
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier
```
`SurfaceDefaults.border()` → `BorderStroke(2.dp, outline)` / `SurfaceDefaults.border(width?, color)` でカスタマイズ可能。
フォーカス時は5.dpに拡大しアニメーション。Surfaceの順序: `.surface().padding()` の順で適用。

**Card** (3オーバーロード: focusable / clickable / action付き)
```kotlin
@Composable
fun Card(
    // clickableの場合: onClick: () -> Unit,
    // actionの場合: action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    header: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = CardDefaults.shape,  // shapes.medium
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = CardDefaults.contentPadding,  // medium
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
)
```
最小高さ: 80.dp、ヘッダーのアスペクト比上限: 1.6:1。
`CardDefaults.contentPadding` / `CardDefaults.shape` でデフォルト値参照。

**ListItem** (2オーバーロード: focusable / clickable)
```kotlin
@Composable
fun ListItem(
    // clickableの場合: onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingLabel: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = GlimmerTheme.shapes.medium,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = ListItemDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
)
```

**TitleChip** (非インタラクティブ、非フォーカス)
```kotlin
@Composable
fun TitleChip(
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = GlimmerTheme.shapes.large,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = TitleChipDefaults.contentPadding,
    content: @Composable RowScope.() -> Unit,
)
```
最小高さ: 44.dp、最大幅: 352.dp、captionタイポグラフィ使用。
`TitleChipDefaults.associatedContentSpacing` — 後続コンテンツとのスペーシング。

**Icon** (6オーバーロード: ImageVector/ImageBitmap/Painter × with/without tint)
```kotlin
@Composable
fun Icon(
    imageVector: ImageVector,  // or ImageBitmap, or Painter
    contentDescription: String?,
    modifier: Modifier = Modifier,
)
// tint版は追加でtint: Colorパラメータ
// Painter版はColorProducerによるtintもサポート
```

**VerticalList** (2オーバーロード: with/without title)
```kotlin
@Composable
fun VerticalList(
    modifier: Modifier = Modifier,
    state: ListState = rememberListState(),
    contentPadding: PaddingValues = VerticalListDefaults.contentPadding,
    userScrollEnabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = rememberOverscrollEffect(),
    flingBehavior: FlingBehavior = VerticalListDefaults.flingBehavior(state),
    reverseLayout: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = VerticalListDefaults.verticalArrangement,
    content: ListScope.() -> Unit,
)

// タイトル付きオーバーロード
@Composable
fun VerticalList(
    title: @Composable () -> Unit,
    // ... 同じパラメータ
)
```
- `VerticalListDefaults.itemSpacing` = 20.dp (extraLarge)
- `ListScope` で `item(key?, contentType?, content)` / `items(count, key?, contentType?, itemContent)` を使用
- 拡張関数: `ListScope.items(items: List<T>, ...)` / `ListScope.itemsIndexed(items: List<T>, ...)`
- `ListState` — `rememberListState()`で状態保持、`scrollToItem()` / `animateScrollToItem()` (suspend)
- フォーカスベースのスナップスクロール、エッジフェード（スクリム）自動適用

**VerticalStack** — カードスタック（フリップ式）コンテナ
```kotlin
@Composable
fun VerticalStack(
    modifier: Modifier = Modifier,
    state: StackState = rememberStackState(),
    content: StackScope.() -> Unit,
)
```
- `StackScope.item(key?, content)` / `StackScope.items(count, key?, itemContent)`
- `StackState` — `rememberStackState()`で状態保持、`topItem`, `scrollToItem()`, `animateScrollToItem()`

##### Modifier拡張

**onIndirectPointerGesture** — タッチパッドジェスチャー検出
```kotlin
fun Modifier.onIndirectPointerGesture(
    enabled: Boolean = true,
    onSwipeForward: (() -> Unit)? = null,
    onSwipeBackward: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
): Modifier
```
タッチパッドのスワイプ前方/後方、クリックを検出。`.focusTarget()`と併用が必要。

**depthEffect** — 深度エフェクト適用
```kotlin
fun Modifier.depthEffect(depthEffect: DepthEffect?, shape: Shape): Modifier
```

**contentColorProvider** — コンテンツ色の伝播
```kotlin
fun Modifier.contentColorProvider(contentColor: Color): Modifier
```

##### 重要な内部メカニズム

- `calculateContentColor(backgroundColor)` — 背景色から最適なコンテンツ色を自動算出
- フォーカス: アウトラインベース（リップルではない）、回転グラデーションハイライト（API 33+のAGSLシェーダー）
- ボーダー: 非フォーカス=2.dp、フォーカス=5.dpでアニメーション
- プレス状態: 最小表示時間あり
- NoIndication — デフォルトリップルを無効化するカスタムIndication

---
## UI設計ガイドライン（Glimmer）

### 基本原則
- 背景は常に黒/透明（`Color.Black`=透過ディスプレイでは透明）
- コンテンツは明るい色でハイコントラスト
- フォーカス状態はGlimmerが自動管理（アウトラインベース、リップルなし）
- `calculateContentColor()`が背景色から適切なテキスト色を算出

### コンポーネント使用ルール
- **リスト**: 3アイテム以下を表示。`VerticalList`を使う（`LazyColumn`は使わない）
- **カードスタック**: ページめくり型UIには`VerticalStack`を使う
- **TitleChip**: 読み取り専用、非インタラクティブな短いラベル用（最大352.dp幅）
- **Button**: `ButtonSize.Medium`(48.dp) / `ButtonSize.Large`(72.dp)、先頭/末尾アイコン対応
- **Card**: 最小80.dp高、title/subtitle/header/icon スロット
- **Surface**: Modifier拡張として使用。デフォルトボーダーは2.dp。`.surface().padding()`の順で適用

### 入力設計
- **音声入力を最優先**: Gemini統合で音声コマンドを主要フローにする
- **タッチパッドをフォールバック**: `onIndirectPointerGesture`でスワイプ/クリック検出
- **カメラボタン**: `ProjectedInputAction.TOGGLE_APP_CAMERA`で検出

### プレビュー
- Android Studioの@Previewアノテーションで`GlimmerTheme`をラップして確認
- 推奨プレビューサイズ: ~400×800dp（AIグラスのFOVに近い）

---
