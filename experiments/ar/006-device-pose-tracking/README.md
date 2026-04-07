# 006: デバイスポーズトラッキング基礎

## 仮説
XR Runtime SessionとARCore ArDeviceを使って、AIグラスのデバイス姿勢(position+rotation)をリアルタイムで取得し、Glimmer UIに表示できる。

## 使用技術
- 使用したSkill: xr-runtime, glimmer-api, glasses-arch
- 主要ライブラリ: androidx.xr.runtime (Session, Config, TrackingState), androidx.xr.arcore (ArDevice), androidx.xr.runtime.math (Pose, Vector3, Quaternion)

## 実装内容
- Session.create() + Config(deviceTracking=SPATIAL_LAST_KNOWN)でトラッキング初期化
- ArDevice.getInstance().state.collect{}でリアルタイムポーズ収集
- TrackingStateのwhen式による網羅的状態管理
- TitleChipでトラッキング状態、Cardでposition/rotation値を表示
- SessionCreateResultのsealed classハンドリング

## 実行方法
1. Android Studio Canaryで開く
2. スマートフォンAVDとAIグラスAVDを起動
3. スマートフォンAVDをターゲットに実行

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
（合格後にpatterns/に抽出したものがあれば参照リンク）
