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

## 前回の差し戻し理由（human-feedback 2026-04-06 20:13:40）

Hiromaruによるエミュレータ動作確認で以下の問題が報告された:

1. **UI再表示不可バグ（深刻度: high）**: エミュ起動時にはUIが見えるが、一回別の画面に行ったり、リセットしたりすると同じセッションではどれだけアプリを再起動したりしてもUIが表示されない。
2. **テキスト中央ずれ**: テキストがほんの少し中央からずれているように見える。

**改善方向の指示**:
- いつでもアプリが再起動できるようにしたい
- テキストを中央に揃えて

## 今回の修正内容（フィードバック対応 #3）

### 修正1: UI再表示不可バグの根本解決

**原因分析**: GlassesMainActivityが別画面に遷移した後、バックスタックに残った既存インスタンスが再利用される際、DisplayControllerがクローズ済み（またはnull）になっていたにもかかわらず、再初期化が行われていなかった。onCreate時のみの初期化では、Activityのライフサイクル遷移（onStop→onResume）に対応できない。

**対策（3層の防御）**:
1. **AndroidManifest: `launchMode="singleTop"`** — 既存インスタンスがタスク先頭にある場合、新インスタンスを作らずonNewIntentで再利用する
2. **`onNewIntent()` での再初期化** — singleTopで再利用された場合、DisplayControllerを解放→再初期化
3. **`onResume()` での復帰チェック** — displayControllerがnullなら再初期化。バックグラウンドからの復帰にも対応
4. **MainActivityからの起動時に `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`** — 既存Activityがある場合はスタックをクリアして再利用

### 修正2: テキスト中央ずれの修正

**原因分析**: VerticalListのデフォルト`horizontalAlignment`が`Alignment.Start`（左揃え）であるため、Column内の`Alignment.CenterHorizontally`設定と不一致が生じ、リスト部分が左にずれて見えていた。また、Boxの`contentAlignment`が`TopCenter`で上端に寄っていた。

**対策**:
1. **VerticalListに `horizontalAlignment = Alignment.CenterHorizontally` を明示** — リスト内アイテムを中央揃えに
2. **BoxのcontentAlignmentを `Alignment.Center` に変更** — コンテンツ全体を画面中央に配置
3. **Cardに `Modifier.fillMaxWidth()` を追加** — カードが親の幅いっぱいに広がり、内部コンテンツの中央配置が視覚的に均等になる

### 修正3: ログ出力の追加

デバッグ・問題調査用にライフサイクル各フェーズでのログ出力を追加。DisplayControllerの初期化/解放タイミングを追跡可能にした。

### 修正4: エラーハンドリングの強化

- `initializeGlassesFeatures()` にtry-catchを追加。初期化失敗時もクラッシュしない
- `releaseDisplayController()` にtry-catchを追加。close()失敗時もクラッシュしない
- `launchGlassesActivity()` にtry-catchを追加

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
8. **再表示テスト**: ホーム画面に戻ってから再度アプリを起動し、UIが表示されることを確認

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
- [patterns/ui-patterns.md](../../patterns/ui-patterns.md) - Glimmer基本コンポーネント配置パターン
- [patterns/architecture-patterns.md](../../patterns/architecture-patterns.md) - 2アクティビティ起動パターン
