# XR R&D ハーネス 運用手順書

## 全体像: 何が手動で何が自動か

```
あなた（手動）              Claude Code（自動）
──────────────              ────────────────
1. dashboard起動            
2. チケット起票（WebUI）    
3. "orchestratorを実行"     → Builder → Tester → Reviewer → 品質ゲート
   と一言入力するだけ         → git push / archive まで全自動
4. 結果をWebUIで確認        
5. 必要に応じてフィードバック
```

**あなたがやること**: ダッシュボード起動、チケット起票、「実行」の一言、結果確認
**Claude Codeがやること**: それ以外の全部

---

## セットアップ（初回のみ）

### 1. リポジトリに配置

```bash
# ZIPを解凍してプロジェクトルートに配置
unzip xr-rnd-harness.zip -d /path/to/your/xr-project
cd /path/to/your/xr-project

# gitリポジトリ初期化（まだなら）
git init
git add .
git commit -m "initial: R&D harness setup"
```

### 2. Python依存インストール（dashboardに必要）

```bash
pip install pyyaml markdown
```

### 3. AVD名の確認

```bash
emulator -list-avds
```

表示されたAVD名を確認し、スマホAVDとグラスAVDの名前を控える。
デフォルトの `phone_avd` / `glasses_avd` と違う場合:

```bash
# プロジェクトルートに作成
cat > .env.emulator << EOF
PHONE_AVD="あなたのスマホAVD名"
GLASSES_AVD="あなたのグラスAVD名"
EOF
```

### 4. scrcpy-mcp のセットアップ（エミュレータ動作確認に必要）

```bash
# scrcpyインストール（まだなら）
# Windows: scoop install scrcpy  or  choco install scrcpy
# Mac: brew install scrcpy
# Linux: apt install scrcpy

# Claude Codeにscrcpy-mcpを追加
claude mcp add android npx scrcpy-mcp
```

確認:
```bash
# Claude Codeを起動して /mcp で接続確認
claude
# > /mcp
# android (scrcpy-mcp) が表示されればOK
```

### 5. settings.local.json の配置

```bash
# すでにZIPに含まれているが、.claude/ 内にも必要な場合
cp settings.local.json .claude/settings.local.json
```

---

## 日常の運用フロー

### Step 1: ダッシュボード起動

```bash
python3 dashboard.py --port 5000
```

ブラウザで http://localhost:5000 を開く。
（ダッシュボードはバックグラウンドで起動しっぱなしにしておくと便利）

```bash
# バックグラウンド起動
nohup python3 dashboard.py --port 5000 &
```

### Step 2: チケット起票

**方法A: WebUI（推奨）**
http://localhost:5000/tickets/new からフォームで起票。

**方法B: 手動YAML**
`tickets/` に直接YAMLファイルを作成。テンプレートは `tickets/.templates/ticket.yaml` を参照。

**方法C: Plannerに任せる**
Claude Codeで:
```
plannerに次のチケットを5枚生成させて
```

### Step 3: パイプライン実行

Claude Codeを起動して、一言:

```
orchestratorでqueuedのチケットを処理して
```

これだけ。以降は全自動:
1. Orchestratorがqueuedチケットをピック
2. Builderが実装
3. Testerが静的チェック（+ emulatorテスト）
4. Reviewerが品質採点
5. PASS → git push + パターン抽出 / FAIL → archive

**エミュレータが必要な場合（verification_level=emulator）:**
Testerがエミュレータテストフェーズに入る前に、エミュレータが起動している必要がある。

```
/launch-emulator
```

を事前に実行しておくか、Claude Codeに「エミュレータ起動してからテストして」と指示。

### Step 4: 結果確認

ダッシュボード（http://localhost:5000）で確認:
- **Dashboard**: 全体のステータスサマリ + フィードバック統計
- **Tickets**: 全チケット一覧、ステータス変更も可能（FBフラグ表示あり）
- **Experiments**: カテゴリ別にREADME、テスト結果、レビュースコア、差し戻しボタン
- **Feedback**: フィードバック履歴一覧 + 深刻度別統計 + アーカイブ
- **Patterns**: 蓄積された実装パターン集
- **Registry**: 完了済み機能一覧 + 失敗分析

### Step 5: 人間の動作確認＆フィードバック

PASS済みの実験を実際にエミュレータや実機で確認して、微妙だったら差し戻す。

**方法A: WebUI（推奨）**

1. http://localhost:5000/experiments を開く
2. 対象の実験カードの「差し戻し」ボタンをクリック
3. フォームに以下を入力:
   - 理由（概要）
   - 深刻度（low / medium / high / critical）
   - 具体的な問題点（1行ずつ）
   - 改善方向（Builderへの指示）
4. 「差し戻し送信」をクリック

送信すると自動で以下が実行される:
1. `human-feedback.yaml` が生成される
2. REGISTRY.mdとpatterns/から該当エントリが削除される
3. `archive/human-rejections.md` に教訓が記録される
4. チケットが `queued` に戻り `human_feedback: true` フラグが立つ
5. 次にOrchestratorが処理するとき、**フィードバック付きチケットが最優先でピックされ**、Builderがフィードバックを読んで改修

フィードバック履歴は http://localhost:5000/feedback で確認できる。

**方法B: Claude Codeで一言**
```
実験001を差し戻し。理由: フォーカスのアウトラインが見えない
```

```
experiments/ui/002をreject。スワイプの反応が悪い
```

WebUIと同じ処理が実行される。どちらの方法でも結果は同じ。

**PDCAのサイクル**:
```
Plan:  チケット起票（WebUI or Planner）
Do:    Builder実装 → Tester → Reviewer → PASS
Check: あなたが実際に動かして確認
Act:   微妙なら差し戻し（WebUI or CLI）→ Builderが改修 → 再テスト → 再レビュー
```

**差し戻しのコツ**:
- 理由は具体的に書く。「微妙」だけだとBuilderが何を直すかわからない
- 「フォーカスが見えない」「スワイプが3回に1回しか反応しない」「文字が小さすぎる」など
- 深刻度を正しく設定する。critical/highは根本作り直し、lowはピンポイント修正になる
- 「改善方向」は具体的に。「もっと明るく」より「#FFFFFFの2pxボーダー」の方がBuilderに伝わる
- 同じ理由で2回差し戻すと、Builderはその問題パターンを学習して次回から注意する

---

## よく使うコマンド

| やりたいこと | Claude Codeに入力 |
|------------|------------------|
| パイプライン実行 | `orchestratorでqueuedのチケットを処理して` |
| チケット自動生成 | `plannerに次のチケットを生成させて` |
| エミュレータ起動 | `/launch-emulator` |
| 特定チケット実行 | `チケット001を処理して` |
| 実験を差し戻し | `実験001を差し戻し。理由: フォーカスが見えない` |
| レビュー結果確認 | `最新のレビュー結果を見せて` |
| パターン追加 | `この実装からパターンを抽出して` |

---

## エミュレータについて

### いつ必要か

| チケットのverification_level | エミュレータ必要？ |
|-----|------|
| `static` | 不要。ビルド＋静的チェックのみ |
| `emulator` | 必要。UIや入力の動作確認 |
| `device` | 不要（実機待ち。ビルドチェックのみ） |

### 起動・停止

```bash
# 起動（/launch-emulator コマンドでも可）
emulator @phone_avd -no-boot-anim &
# ブート完了を待つ
adb wait-for-device
adb shell 'while [[ "$(getprop sys.boot_completed)" != "1" ]]; do sleep 1; done'
emulator @glasses_avd -no-boot-anim &

# 停止
adb devices | grep emulator | cut -f1 | while read line; do adb -s "$line" emu kill; done
```

### 注意事項
- **起動順序**: スマホAVD → グラスAVD（この順番必須）
- **初回ペア設定は手動で完了済み**（以降は自動再接続）
- **カメラ機能はエミュレータ非対応** → `verification_level: device` にすること

---

## トラブルシューティング

| 症状 | 対処 |
|------|------|
| ビルドが通らない | Android SDK, JDK, Gradleのバージョンを確認。`glasses-arch` スキルのbuild.gradle.kts設定を参照 |
| エミュレータが起動しない | `emulator -list-avds` でAVD名を確認。HAXM/KVMが有効か確認 |
| ペア設定が切れた | グラスAVDのデータ消去→コールドブート→再ペア設定 |
| Testerが全部FAILする | まずチケット001を手動で `./gradlew assembleDebug` して環境確認 |
| ダッシュボードが開かない | `pip install pyyaml markdown` を確認。ポート5000が空いているか確認 |

---

## PCつけっぱなしで自動R&D

長時間放置でR&Dを回し続けたい場合:

```bash
# ターミナル1: ダッシュボード
python3 dashboard.py --port 5000 &

# ターミナル2: Claude Codeでorchestrator実行
claude
# → "orchestratorで全チケットを連続処理して。終わったらplannerに5枚生成させて続行"
```

Orchestratorはチケットが尽きるとPlannerに自動で次のチケット生成を依頼するので、
理論上はPCが起きている間ずっとR&Dが回り続ける。

**ただし現実的な注意点:**
- Claude Codeのセッションにはトークン上限がある。長時間連続は途切れる可能性がある
- エミュレータテストが必要なチケットはエミュレータが起動している必要がある
- 定期的にダッシュボードで結果を確認し、品質が下がっていないかチェック推奨
