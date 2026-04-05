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

### 001: Glimmer基本UIコンポーネント動作確認
- **機能**: GlimmerThemeによる基本UIコンポーネント（Button, Card, ListItem, VerticalList, TitleChip）の描画とタッチパッドフォーカスナビゲーション
- **APIカバレッジ**: GlimmerTheme, Button (Medium/Large), ButtonSize, Card, ListItem, VerticalList, items, TitleChip, Icon, Text, Color.Black背景
- **パターン参照**: patterns/ui-patterns.md#glimmer基本コンポーネント配置
- **PASS日**: 2026-04-05
- **スコア**: 9/10
