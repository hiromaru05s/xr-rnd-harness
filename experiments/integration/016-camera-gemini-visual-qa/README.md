# 016: カメラ+Gemini Live 視覚質問応答

## 仮説
CameraXで撮影した画像をGemini Liveセッションに送信し、音声で質問→視覚的な回答を得る統合パターンが確立できる。

## 使用技術
- 使用したSkill: glimmer-api, projected-api, glasses-hardware, glasses-arch
- 主要ライブラリ: CameraX, Firebase AI (Gemini Live), Projected API, Glimmer

## 実装内容
- カメラボタン押下で画像キャプチャ（ProjectedActivityCompat入力イベント + CameraX）
- キャプチャ画像をBase64エンコードしてGemini LiveセッションにInlineDataとして送信
- 音声質問と画像を組み合わせたマルチモーダル会話ループ
- AppState sealed classによる統合ステートマシン（Initializing/Ready/Capturing/Analyzing/Conversing/Error）
- Glimmer UIで状態表示（TitleChip + Card、FOV準拠）

## 実行方法
1. Android Studio Canaryで開く
2. google-services.jsonをapp/に配置
3. スマートフォンAVDとAIグラスAVDを起動
4. スマートフォンAVDをターゲットに実行

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
（合格後にpatterns/に抽出したものがあれば参照リンク）
