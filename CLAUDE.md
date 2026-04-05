# Android XR AIグラスアプリ R&D

## プロジェクト概要

Android XR SDK を使った**AIグラス専用**アプリのR&D。ヘッドセット(HMD)向けではない。
スマートフォン上のアクティビティとして動作し、グラスの透過ディスプレイにプロジェクションされる。

## 絶対ルール

- 黒背景 = 透明（加算光方式）。暗い背景 + 明るいコンテンツ
- FOV 50-70°。UIはグランス可能で最小限（リスト3アイテム以下）
- 入力: タッチパッド（必須）+ 音声（Gemini）+ カメラボタン。6DoFコントローラーなし
- SceneCore / Compose XR / SpatialAudio 等のHMD専用ライブラリは**絶対に使わない**
- Kotlin + Compose + Gradle Kotlin DSL。compileSdk=36, minSdk=35

## スキル参照（タスク別に必要なものだけ読む）

| やること | 読むスキル |
|---------|-----------|
| UI実装（Glimmerコンポーネント、テーマ、レイアウト） | `glimmer-api` |
| スマホ↔グラス通信、接続状態、入力イベント | `projected-api` |
| ARCore、ポーズトラッキング、Geospatial API | `xr-runtime` |
| build.gradle、Manifest、アーキテクチャ、コードパターン | `glasses-arch` |
| カメラ、TTS、Gemini Live、通知、ハードウェアアクセス | `glasses-hardware` |
| エミュレータ設定、テスト方法、トラブルシュート | `glasses-testing` |
| R&Dパイプライン実行ルール | `rnd-harness` |

## リポジトリ構成

```
CLAUDE.md                    ← このファイル（軽量ルーティング）
REGISTRY.md                  ← 完了済み機能レジストリ（Planner重複防止用）
.claude/
  agents/                    ← カスタムエージェント定義
    planner.md
    builder.md
    tester.md
    reviewer.md
    orchestrator.md
  commands/                  ← スラッシュコマンド
    launch-emulator.md       ← /launch-emulator でエミュレータ一発起動
    reject-experiment.md     ← 人間レビュー差し戻しフロー
  skills/                    ← APIリファレンス・手順書
    glimmer-api/SKILL.md
    projected-api/SKILL.md
    xr-runtime/SKILL.md
    glasses-arch/SKILL.md
    glasses-hardware/SKILL.md
    glasses-testing/SKILL.md
    rnd-harness/SKILL.md
tickets/                     ← チケットキュー（YAML）
  .templates/ticket.yaml
experiments/                 ← 実験実装（カテゴリ別）
  ui/                        ← Glimmer UI、レイアウト、テーマ
  input/                     ← タッチパッド、音声入力、カメラボタン
  voice/                     ← TTS、Gemini Live、音声会話
  camera/                    ← CameraX、画像処理
  ar/                        ← ARCore、Geospatial、ポーズ
  architecture/              ← アプリ構造、ライフサイクル、状態管理
  integration/               ← 複数APIの組み合わせ
  各フォルダ内:
    NNN-experiment-name/
      README.md
      app/
      findings.md
patterns/                    ← 合格した実装から抽出（AI-readable形式）
  各 .md はAIがCLAUDE.mdとして読み込める形式で記述
archive/rejected/            ← 品質ゲート不合格でアーカイブされたもの
  failure-analysis.md        ← 失敗パターン集約（Planner参照用）
  human-rejections.md        ← 人間レビュー差し戻し履歴（PDCA用）
dashboard.py                 ← WebUI（python3 dashboard.py --port 5000）
OPERATIONS.md                ← 運用手順書（人間向け）
```

## 運用方法

詳細は `OPERATIONS.md` を参照。要約:
1. `python3 dashboard.py --port 5000` でダッシュボード起動
2. http://localhost:5000 からチケット起票
3. Claude Codeで `orchestratorでqueuedのチケットを処理して` と入力
4. ダッシュボードで結果確認
5. 微妙だったら `実験001を差し戻し。理由: ○○` で差し戻し → 自動で改修サイクルが回る

## コーディング規約

- Kotlin推奨、Kotlin DSL（build.gradle.kts）
- Composeベースの宣言的UI
- `@OptIn(ExperimentalProjectedApi::class)` をProjected API使用時に付与
- エラーは適切にハンドリング（when式でResult型を網羅）
- 各実験フォルダにREADME.md必須（目的・手順・発見事項）
