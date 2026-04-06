# 001: Glimmer基本UIコンポーネント動作確認

## 仮説
GlimmerThemeの各基本コンポーネント（Button, Card, ListItem, VerticalList, TitleChip）が透過ディスプレイ前提の黒背景で正しく描画され、タッチパッドフォーカスナビゲーションが機能する。

## 使用技術
- 使用したSkill: glimmer-api, glasses-arch, projected-api
- 主要ライブラリ:
  - androidx.xr.glimmer:glimmer:1.0.0-alpha08
  - androidx.xr.projected:projected:1.0.0-alpha05
  - androidx.xr.runtime:runtime:1.0.0-alpha12
- Compose BOM: 2025.01.00
- Kotlin + Compose + Gradle Kotlin DSL (compileSdk=36, minSdk=35)

## 前回の差し戻し理由
Hiromaruによるエミュレータ動作確認で、初回起動後にUIが表示されなくなるバグが発覚:
1. **Manifestの構成ミス**: GlassesMainActivityにLAUNCHERカテゴリとrequiredDisplayCategoryを同時指定。スマホ側MainActivityが存在せず、再起動時にprojected contextなしで起動→描画先がなくUIが出ない。
2. **ディスプレイ制御の欠如**: ProjectedDisplayController/ProjectedDeviceController未使用→FLAG_KEEP_SCREEN_ONが効かず、グラスディスプレイがスヌーズに入る。

## 今回の修正内容

### 修正1: スマホ側 MainActivity 新規作成
- `MainActivity` をLAUNCHERアクティビティとして追加
- `ProjectedContext.isProjectedDeviceConnected()` でグラス接続を監視し自動起動
- `ProjectedContext.createProjectedActivityOptions()` 経由で GlassesMainActivity を起動
- 手動起動ボタンも備えたスマホ側UI

### 修正2: GlassesMainActivity のディスプレイ制御追加
- `GlassesMainActivity` から LAUNCHER カテゴリを削除
- `ProjectedDeviceController` で `CAPABILITY_VISUAL_UI` をチェック
- `ProjectedDisplayController` で `FLAG_KEEP_SCREEN_ON` を設定（スヌーズ防止）
- `PresentationMode.VISUALS_ON` を監視し、ビジュアルオフ時はUIを非表示
- ライフサイクルオブザーバーで DisplayController を適切にクリーンアップ

### 修正3: AndroidManifest.xml を公式推奨パターンに修正
- MainActivity: LAUNCHER カテゴリ付き（スマホ側起動ポイント）
- GlassesMainActivity: LAUNCHER カテゴリなし + `requiredDisplayCategory="xr_projected"`

### 修正4: build.gradle.kts に依存追加
- `androidx.xr.projected:projected:1.0.0-alpha05`
- `androidx.xr.runtime:runtime:1.0.0-alpha12`
- `androidx.xr.projected:projected-testing:1.0.0-alpha05` (androidTest)

## 実装内容
`MainActivity`（スマホ側ランチャー）→ `ProjectedContext` 経由 → `GlassesMainActivity`（グラス側）の2段構成。
GlassesMainActivity に `GlimmerTheme` でラップした `BasicUiScreen` を配置。
以下のコンポーネントを1画面に集約して動作確認:
- `TitleChip`: 選択中アイテムをステータス表示
- `Card` + `Button` (Medium/Large・アイコン付き): アクション実行
- `VerticalList` + `ListItem` x3: タッチパッドフォーカス移動確認

## 実行方法
1. Android Studio Canaryで `experiments/ui/001-glimmer-basic-ui/` を開く
2. スマートフォンAVDとAIグラスAVDを起動してペア設定
3. スマートフォンAVDをターゲットに `MainActivity` を実行（LAUNCHERとして起動）
4. グラスが接続されていれば自動的に GlassesMainActivity がグラスディスプレイに表示
5. 手動起動する場合は「グラスに表示」ボタンをタップ
6. グラスAVDに BasicUiScreen が表示されることを確認
7. タッチパッド操作でフォーカスアウトラインが移動することを確認

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
- [patterns/ui-patterns.md](../../patterns/ui-patterns.md) - Glimmer基本コンポーネント配置パターン
