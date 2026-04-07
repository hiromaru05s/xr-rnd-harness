# 008: TTS音声フィードバックシステム

## 仮説
Android TTSをグラスの音声フィードバックとして使い、UI操作の確認音や状態通知を実装できる。ディスプレイレスモード時にはTTSのみで情報伝達できる。

## 使用技術
- 使用したSkill: glasses-hardware, glimmer-api, projected-api, glasses-arch
- 主要ライブラリ: android.speech.tts.TextToSpeech, UtteranceProgressListener

## 実装内容
- TtsManagerクラス: TTS初期化、QUEUE_FLUSH/QUEUE_ADD、UtteranceProgressListenerによる発話状態追跡
- sealed class TtsState: Initializing/Ready/Speaking/Completed/Errorの5状態
- PresentationMode.VISUALS_ON監視: ディスプレイON/OFFに応じた自動モード切替
- ディスプレイOFF時のTTSフォールバック: 音声のみで情報伝達

## 実行方法
1. Android Studio Canaryで開く
2. スマートフォンAVDとAIグラスAVDを起動
3. スマートフォンAVDをターゲットに実行

## 発見事項
（テスト・レビュー後に追記）
