---
name: launch-emulator
description: >
  AIグラスエミュレータ環境をワンコマンドで起動する。
  スマホAVDとグラスAVDを起動し、ペア接続を待ち、使える状態にする。
  「エミュレータ起動」「テスト環境立ち上げ」等の指示で使う。
---

# AIグラスエミュレータ起動

以下の手順をBashで順番に実行する。

## 前提条件（初回のみ手動セットアップが必要）
- Android Studio Canary がインストール済み
- Android SDK の `ANDROID_HOME` が設定済み
- スマホAVDが `phone_avd` という名前で作成済み
- グラスAVDが `glasses_avd` という名前で作成済み
- 初回ペア設定は手動で完了済み

## 起動スクリプト

```bash
#!/bin/bash
set -e

PHONE_AVD="phone_avd"
GLASSES_AVD="glasses_avd"

echo "=== [1/5] 既存エミュレータの確認 ==="
if adb devices | grep -q "emulator"; then
  echo "既にエミュレータが起動中。スキップ。"
  adb devices
  exit 0
fi

echo "=== [2/5] スマホAVD起動 ==="
emulator @${PHONE_AVD} -no-boot-anim -no-audio -gpu auto &
PHONE_PID=$!
echo "スマホAVD PID: $PHONE_PID"

echo "=== [3/5] スマホAVDブート完了待ち ==="
adb wait-for-device
while [[ "$(adb shell getprop sys.boot_completed 2>/dev/null)" != "1" ]]; do
  sleep 2
done
echo "スマホAVDブート完了"

echo "=== [4/5] グラスAVD起動 ==="
emulator @${GLASSES_AVD} -no-boot-anim -no-audio -gpu auto &
GLASSES_PID=$!
echo "グラスAVD PID: $GLASSES_PID"

echo "=== [5/5] ペア接続待ち（30秒） ==="
sleep 30

echo "=== 起動完了 ==="
adb devices
echo ""
echo "ディスプレイスヌーズ無効化:"
adb shell dumpsys activity service com.google.android.glasses.core/com.google.android.projection.core.app.service.AndroidProjectionCoreService preferences_set pref_automatic_snooze_timeout false
echo ""
echo "準備OK。アプリを実行するには:"
echo "  cd experiments/{category}/NNN-xxx/app"
echo "  ./gradlew installDebug"
```

## 停止スクリプト

```bash
#!/bin/bash
adb devices | grep emulator | cut -f1 | while read line; do
  adb -s "$line" emu kill
done
echo "全エミュレータを停止しました"
```

## AVD名のカスタマイズ
AVD名が `phone_avd` / `glasses_avd` でない場合は、
プロジェクトルートに `.env.emulator` を作成して上書き:

```bash
# .env.emulator
PHONE_AVD="my_phone_name"
GLASSES_AVD="my_glasses_name"
```
