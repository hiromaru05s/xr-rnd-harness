---
name: glasses-testing
description: AIグラスアプリのテスト方法リファレンス。エミュレータセットアップ（スマホAVD＋グラスAVD）、ペア設定、タッチパッド操作、Gemini Live使用方法、Layout Inspector、ディスプレイスヌーズ無効化、トラブルシューティングに関するタスクでは必ずこのスキルを使う。テストやデバッグ時は必ず参照。
---

# AIグラス テスト・エミュレータリファレンス

## テスト

### エミュレータセットアップ
1. **Android Studio最新Canaryビルド**を使用すること（他のバージョンにはXRツールがない）
2. Device Managerでスマートフォン AVDを作成（グラスのホストデバイス）
3. Device ManagerでAI グラス AVDを作成
4. **起動順序**: スマートフォンAVD → AI グラスAVD（この順番で起動）
5. グラスアプリでペア設定を実行（初回のみ、以降は自動再接続）
6. アプリ実行時はスマートフォンAVDをターゲットデバイスとして選択

### エミュレータ操作
- **タッチパッド**: ディスプレイ領域の下部。右側=グラス前面、左側=グラス後面（スワイプ方向に重要）
- **音声入力**: エミュレータコントロールからマイクを有効化（PCのデフォルトマイク使用）
- **ディスプレイレスモード**: グラスアプリ > デバイスの設定 > 音声のみモード → コールドブートで再起動

### ディスプレイスヌーズタイムアウト無効化
```bash
adb shell dumpsys activity service com.google.android.glasses.core/com.google.android.projection.core.app.service.AndroidProjectionCoreService preferences_set pref_automatic_snooze_timeout false
```

### Gemini Live使用（エミュレータ）
1. スマートフォンエミュレータでGoogleアプリを最新版（16.46.63以上）に更新
2. AIグラスエミュレータのタッチパッドを約2秒長押し
3. 初回は権限リクエストをすべて承認し、再度長押し

### Layout Inspector使用
- 埋め込みLayout Inspectorを無効化: Settings > Tools > Layout Inspector > "Enable embedded Layout Inspector"をオフ
- Layout Inspector起動後、AIグラスAVDからスマートフォンAVDに切り替えてグラスUIを検査

### トラブルシューティング
- **ペア設定失敗**: AIグラスAVDのデータを消去してやり直し
- **再接続しない**: スマートフォンをコールドブートしてからグラスを再起動
- **投影アクティビティが起動しない**: デバイスのペア設定をリセット
- **Gemini Live使用不可**: Googleアプリのバージョンを確認・更新

### Composeプレビュー
- Glimmer UIはAndroid StudioのComposableプレビューで確認可能

### テストライブラリ
- `ProjectedTestRule`（JUnit4）でバーチャルデバイス/ディスプレイ作成
- 接続状態、能力、プレゼンテーションモードを制御可能
- `sendProjectedInputEvent()`で入力イベントを注入

---

## 参考リンク

- [Build for AI Glasses](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/build)
- [Create first AI glasses activity](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/first-activity)
- [Jetpack Compose Glimmer](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer)
- [Glimmer What's Included](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/whats-included)
- [Glimmer Lists](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/lists)
- [Glimmer Buttons](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/buttons)
- [Glimmer Icons](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/icons)
- [Glimmer Text](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/text)
- [Glimmer Focus](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/focus)
- [Glimmer Styles Overview](https://developer.android.com/design/ui/ai-glasses/guides/styles/overview)
- [Glimmer API Reference](https://developer.android.com/reference/kotlin/androidx/xr/glimmer/package-summary)
- [Jetpack Projected](https://developer.android.com/jetpack/androidx/releases/xr-projected)
- [Projected Context](https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context)
- [XR Runtime](https://developer.android.com/jetpack/androidx/releases/xr-runtime)
- [ARCore for Jetpack XR](https://developer.android.com/jetpack/androidx/releases/xr-arcore)
- [Android XR Devices](https://developer.android.com/develop/xr/devices)
- [AI Glasses Emulator](https://developer.android.com/develop/xr/jetpack-xr-sdk/run/emulator/ai-glasses)
- [Android XR SDK DP3 Blog](https://android-developers.googleblog.com/2025/12/build-for-ai-glasses-with-android-xr.html)
- [TTS Audio Output](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/tts)
- [Gemini Live API Integration](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/gemini-live)
- [Firebase AI Logic - Gemini Live API](https://firebase.google.com/docs/ai-logic/gemini-live-api)
- [Hardware Access (Projected Context)](https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context)
- [Hardware Permissions](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/permissions)
- [Notification Bridging](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/notifications)
- [Device Pose Tracking](https://developer.android.com/develop/xr/jetpack-xr-sdk/arcore/device-pose)
- [Geospatial API](https://developer.android.com/develop/xr/jetpack-xr-sdk/arcore/geospatial)
- [AI Glasses Emulator Troubleshooting](https://developer.android.com/develop/xr/jetpack-xr-sdk/run/emulator/ai-glasses-troubleshooting)
- [Layout Inspector for AI Glasses](https://developer.android.com/develop/xr/jetpack-xr-sdk/debug/layout-inspector)
