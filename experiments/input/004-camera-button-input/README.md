# 004: カメラボタン入力イベント処理

## 仮説
ProjectedActivityCompatのprojectedInputEventsフローでカメラボタン(TOGGLE_APP_CAMERA)の押下を検出し、UIに反映できる。

## 使用技術
- 使用したSkill: projected-api, glimmer-api, glasses-arch
- 主要ライブラリ: androidx.xr.projected (ProjectedActivityCompat, ProjectedInputEvent, ProjectedInputAction), androidx.xr.glimmer

## 実装内容
- ProjectedActivityCompat.create()でサービスに接続し、projectedInputEventsフローを収集
- ProjectedInputAction.TOGGLE_APP_CAMERAイベントを検出してカウンターをインクリメント
- TitleChipでイベント監視状態を表示、Cardで押下回数と最新イベント情報を表示
- リセットボタンでカウンターを初期化
- 堅牢なライフサイクル管理: ProjectedActivityCompatのclose()をonDestroy時に実行

## 実行方法
1. Android Studio Canaryで開く
2. スマートフォンAVDとAIグラスAVDを起動
3. スマートフォンAVDをターゲットに実行
4. グラスAVDのカメラボタンを押下して動作確認

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
（合格後にpatterns/に抽出したものがあれば参照リンク）
