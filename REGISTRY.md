# 完了済み機能レジストリ

Orchestratorが実験PASS時に自動更新する。Plannerはチケット起票前に必ずこのファイルを確認し、重複する実験を生成しない。

## フォーマット

各エントリ:
- **ID**: 実験番号
- **機能**: 何ができるようになったか
- **APIカバレッジ**: 使用したAPI/コンポーネント
- **パターン参照**: patterns/ 内の該当セクション
- **PASS日**: PASS日とスコア

---

## 完了済み機能

### 001: Glimmer基本UIコンポーネント動作確認
- **PASS日**: 2026-04-06 (10/10) [FB resolved]
- **機能**: GlimmerThemeの基本コンポーネント（Button, Card, ListItem, VerticalList, TitleChip）の描画確認。スマホ→グラスの2アクティビティ起動パターン確立。DisplayControllerの堅牢なライフサイクル管理パターン確立。
- **APIカバレッジ**:
  - `GlimmerTheme`, `Button` (Medium/Large), `Card` (action付き), `ListItem` (onClick付き), `VerticalList` + `items()`, `TitleChip`, `Icon`, `Text`
  - `ProjectedContext.createProjectedActivityOptions()`, `ProjectedContext.isProjectedDeviceConnected()`
  - `ProjectedDeviceController` (CAPABILITY_VISUAL_UI)
  - `ProjectedDisplayController` (FLAG_KEEP_SCREEN_ON, PresentationMode.VISUALS_ON)
  - `@OptIn(ExperimentalProjectedApi::class)`
- **パターン参照**:
  - patterns/ui-patterns.md: Glimmer基本コンポーネント配置, TitleChipステータス表示, Card+Button組み合わせ
  - patterns/architecture-patterns.md: スマホ→グラス2アクティビティ起動パターン, GlassesMainActivity堅牢ライフサイクルパターン
