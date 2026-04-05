# 001: Glimmer基本UIコンポーネント動作確認

## 仮説
GlimmerThemeの各基本コンポーネント（Button, Card, ListItem, VerticalList, TitleChip）が透過ディスプレイ前提の黒背景で正しく描画され、タッチパッドフォーカスナビゲーションが機能する。

## 使用技術
- 使用したSkill: glimmer-api, glasses-arch
- 主要ライブラリ: androidx.xr.glimmer:glimmer:1.0.0-alpha08
- Compose BOM: 2025.01.00
- Kotlin + Compose + Gradle Kotlin DSL (compileSdk=36, minSdk=35)

## 実装内容
`GlassesMainActivity` に `GlimmerTheme` でラップした `BasicUiScreen` を配置。
以下のコンポーネントを1画面に集約して動作確認:
- `TitleChip`: 選択中アイテムをステータス表示
- `Card` + `Button` (Medium/Large・アイコン付き): アクション実行
- `VerticalList` + `ListItem` x3: タッチパッドフォーカス移動確認

タッチパッドナビゲーションはGlimmerのフォーカスシステムが自動管理（アウトラインベース）。

## 実行方法
1. Android Studio Canaryで `experiments/ui/001-glimmer-basic-ui/` を開く
2. スマートフォンAVDとAIグラスAVDを起動してペア設定
3. スマートフォンAVDをターゲットに `GlassesMainActivity` を実行
4. グラスAVDに BasicUiScreen が表示されることを確認
5. タッチパッド操作でフォーカスアウトラインが移動することを確認

## 発見事項
- GlimmerThemeでラップするだけでフォーカスシステムが有効化（追加設定不要）
- VerticalList + items() でリスト表示。LazyColumnは使わない
- DPAD_DOWN/UPでアウトラインフォーカスが自動移動（focusRequester不要）
- Card: title/action/contentの3スロット構成
- Button: Medium/Largeサイズ + leadingIconスロット確認
- TitleChip: leadingIcon + contentでステータスバー的表示
- Color.Black = 加算光方式で透明。UIコンテンツのみ視認可能

詳細は [findings.md](findings.md) を参照。

## 抽出パターン
- [patterns/ui-patterns.md](../../patterns/ui-patterns.md) - Glimmer基本コンポーネント配置パターン
