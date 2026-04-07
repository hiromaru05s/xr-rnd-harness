# 005: 通知ブリッジングとProjectedExtender

## 仮説
NotificationCompatとProjectedExtenderを使って、スマホからグラスへの通知ブリッジングが実装でき、グラス固有のインテントを設定できる。

## 使用技術
- 使用したSkill: projected-api, glasses-hardware, glimmer-api, glasses-arch
- 主要ライブラリ: androidx.xr.projected (ProjectedExtender), NotificationCompat, NotificationChannel

## 実装内容
- NotificationHelperクラスで通知操作を集約（チャンネル作成、標準通知、MessagingStyle通知）
- ProjectedExtenderでグラス固有のcontentIntentを設定
- IMPORTANCE_HIGHチャンネルでブリッジング条件を満たす
- sealed classでNotificationResultを型安全にハンドリング
- when式で結果を網羅的に処理

## 実行方法
1. Android Studio Canaryで開く
2. スマートフォンAVDとAIグラスAVDを起動
3. スマートフォンAVDをターゲットに実行
4. グラス上で「標準通知」「会話通知」ボタンで通知を発行

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
（合格後にpatterns/に抽出したものがあれば参照リンク）
