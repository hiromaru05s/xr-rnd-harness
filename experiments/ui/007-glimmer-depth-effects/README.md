# 007: Glimmer DepthEffect/Surface高度活用

## 仮説
GlimmerのDepthEffectレベルとSurface Modifier、カスタムカラー/ボーダーを組み合わせることで、透過ディスプレイ上で奥行き感のあるUI階層を表現できる。

## 使用技術
- 使用したSkill: glimmer-api, glasses-arch
- 主要ライブラリ: Glimmer (Surface, SurfaceDefaults, Card, shapes, colors)

## 実装内容
- デフォルトSurface/Card: 標準ボーダー(2dp)とsurface色の確認
- カスタムSurface: SurfaceDefaults.border(width, color)でボーダーカスタマイズ
- Modifier.surface(): focusable=trueでフォーカス対応Surface、shapes.smallの角丸
- Card全スロット活用: title, subtitle, leadingIcon, border, color
- GlimmerTheme.colors各色の活用: surface, primary, positive

## 実行方法
1. Android Studio Canaryで開く
2. スマートフォンAVDとAIグラスAVDを起動
3. スマートフォンAVDをターゲットに実行

## 発見事項
（テスト・レビュー後に追記）
