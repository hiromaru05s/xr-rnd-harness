# 完了済み機能レジストリ

Orchestratorが実験PASS時に自動更新する。Plannerはチケット起票前に必ずこのファイルを確認し、重複する実験を生成しない。

## フォーマット

各エントリ:
- **ID**: 実験番号
- **機能**: 何ができるようになったか
- **APIカバレッジ**: 使用したAPI/コンポーネント
- **パターン参照**: patterns/ 内の該当セクション

---

## 完了済み機能

（まだなし。実験がPASSするごとにOrchestratorが追記する）

<!-- 記入例:
### 001: Glimmer基本UIコンポーネント
- **機能**: Button, Card, ListItem, VerticalList, TitleChipの基本描画とフォーカスナビ
- **APIカバレッジ**: GlimmerTheme, Button, Card, ListItem, VerticalList, TitleChip, calculateContentColor
- **パターン参照**: patterns/ui-patterns.md#基本コンポーネント配置
- **PASS日**: 2026-04-05
- **スコア**: 8/10
-->
