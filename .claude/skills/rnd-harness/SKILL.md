---
name: rnd-harness
description: R&Dパイプラインの実行ルール。チケット起票、実験実装、テスト、レビュー、品質ゲート、git push、パターン抽出の全フローを定義。R&Dタスクの実行、チケット管理、実験の開始・完了・アーカイブに関する操作では必ずこのスキルを使う。
---

# R&D ハーネス実行ルール

## パイプライン概要

```
チケット起票 → Orchestrator → Builder → Tester → Reviewer → 品質ゲート
                                  ↑        │         │         ├─ PASS → git push + パターン抽出
                                  │        │         │         ├─ CONDITIONAL → Builder差し戻し（レビューretry最大2回）
                                  │        └─ FAIL ──┘         └─ FAIL(≤4) → 即archive
                                  └── テストFAIL: Builder修正→再テスト（最大5回、超えたらarchive）
```

## フェーズ1: チケット起票

チケットは2つのソースから生成される:
- **手動**: Hiromaruが `tickets/` に YAML ファイルを作成
- **自動**: Planner agent が技術調査・市場動向から生成

### チケットフォーマット (YAML)

```yaml
id: "NNN"
title: "実験タイトル"
category: "ui | input | voice | camera | ar | architecture | integration"
priority: "high | medium | low"
hypothesis: "この実験で検証したい仮説"
scope:
  - "実装すべき具体的な機能1"
  - "実装すべき具体的な機能2"
skills_needed:
  - "glimmer-api"
  - "projected-api"
success_criteria:
  - "成功とみなす条件1"
  - "成功とみなす条件2"
verification_level: "static | emulator | device"
  # static:    ビルド＋静的チェックのみ（ロジック、アーキテクチャ、データ処理系）
  # emulator:  エミュレータでの動作確認が必要（UI、入力、音声系）
  # device:    実機でしか確認できない（カメラ、Geospatial等）→ 実機入手まで保留可
estimated_complexity: "small | medium | large"
status: "queued | in-progress | testing | review | passed | failed | archived"
retry_count: 0
```

## フェーズ2: Builder（実装）

Builderは以下を生成する:

```
experiments/{category}/NNN-experiment-name/
├── README.md          ← 必須（下記テンプレ参照）
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/.../*.kt
│   │   └── res/...
│   └── src/test/      ← ユニットテスト
└── findings.md        ← テスト・レビュー後に追記
```

`{category}` はチケットの `category` フィールドに対応（ui, input, voice, camera, ar, architecture, integration）。

### README.mdテンプレート

```markdown
# NNN: 実験タイトル

## 仮説
（チケットのhypothesisをここに）

## 使用技術
- 使用したSkill: glimmer-api, projected-api, ...
- 主要ライブラリ: ...

## 実装内容
（何を作ったかの簡潔な説明）

## 実行方法
1. Android Studio Canaryで開く
2. スマートフォンAVDとAIグラスAVDを起動
3. スマートフォンAVDをターゲットに実行

## 発見事項
（テスト・レビュー後に追記）

## 抽出パターン
（合格後にpatterns/に抽出したものがあれば参照リンク）
```

### Builderのルール
- チケットの `skills_needed` に記載されたスキルを読む
- **暗黙の必須**: `glasses-arch`（プロジェクト構造）と `rnd-harness`（READMEテンプレ）は常に読む
- CLAUDE.mdの絶対ルールを常に遵守
- HMD専用ライブラリを絶対に使わない
- `LazyColumn` ではなく `VerticalList` を使う
- 背景は常に `Color.Black`（透過ディスプレイ）

## フェーズ3: Tester（テスト）— ここで「動くかどうか」を篩にかける

Testerの役割は**品質判断ではない**。動くか動かないかの二値判定。
ここを通過しないものはReviewerに渡さない。

### 必須チェック（全部PASSしないとFAIL）

| # | チェック項目 | 方法 | 判定 |
|---|------------|------|------|
| T1 | ビルド成功 | `./gradlew assembleDebug` exit code 0 | PASS/FAIL |
| T2 | Lint エラーゼロ | `./gradlew lint` で error 0 | PASS/FAIL |
| T3 | HMD専用ライブラリ不使用 | grep で SceneCore, SpatialAudio, Compose XR が含まれないこと | PASS/FAIL |
| T4 | Glimmer必須ルール準拠 | grep で LazyColumn 不使用、Color.Black 背景使用を確認 | PASS/FAIL |
| T5 | README.md 存在 | ファイル存在チェック | PASS/FAIL |
| T6 | compileSdk/minSdk正しい | build.gradle.kts の値を確認 | PASS/FAIL |
| T7 | ユニットテスト | src/test/にテストがあれば `./gradlew test` 実行。なければSKIP | PASS/FAIL/SKIP |

### テスト結果ファイル

```yaml
# experiments/NNN-xxx/test-result.yaml
experiment_id: "NNN"
timestamp: "2026-04-05T12:00:00Z"
checks:
  T1_build: PASS
  T2_lint: PASS
  T3_no_hmd_libs: PASS
  T4_glimmer_rules: PASS
  T5_readme_exists: PASS
  T6_sdk_versions: PASS
  T7_unit_tests: PASS  # or SKIP
overall: PASS  # SKIPはPASS扱い。1つでもFAILなら全体FAIL
```

### TesterがFAILを出した場合
- `overall: FAIL` の場合、Reviewerには渡さない
- 失敗したチェック項目をBuilderにフィードバック
- Builderが修正 → 再度Tester実行
- **テストのリトライは無制限**（ビルドが通るまでやる）
- ただしOrchestratorが判断して、5回以上テスト失敗したら打ち切ってarchive

## フェーズ4: Reviewer（品質評価）— 動くものだけを評価

**Testerを通過した（＝動作確認済みの）実装に対してのみ品質評価を行う。**

### 評価基準（10点満点）

| 基準 | 配点 | 評価ポイント |
|------|------|------------|
| AIグラスUX適合度 | 3点 | グランス可能か？情報量は最小限か？音声/タッチパッドの入力設計は適切か？FOV制約を考慮しているか？ |
| コード品質 | 3点 | Kotlin慣用句（scope functions, sealed class, coroutines）、エラーハンドリング網羅性、関心の分離、命名規則 |
| 再利用性・パターン化可能性 | 2点 | 他の実験で再利用できる設計か？パターンとして抽出可能な汎用性があるか？ |
| ドキュメント品質 | 2点 | README.mdが仮説・手順・発見を明確に記述しているか？コード内コメントは適切か？ |

### スコアリングルール
- 各基準は 0〜配点満点 の整数で採点
- **7/10以上: PASS** → git push + パターン抽出
- **5〜6/10: CONDITIONAL** → 具体的な改善指摘付きでBuilderに差し戻し（リトライ1回目）
- **4/10以下: FAIL** → 即アーカイブ（根本的に設計が悪い）

### レビュー結果ファイル

```yaml
# experiments/NNN-xxx/review-result.yaml
experiment_id: "NNN"
timestamp: "2026-04-05T12:30:00Z"
scores:
  ux_fitness: 2        # /3
  code_quality: 3      # /3
  reusability: 1       # /2
  documentation: 2     # /2
total: 8               # /10
verdict: "PASS"        # PASS | CONDITIONAL | FAIL
feedback: |
  UX: タッチパッドナビゲーションは実装されているが、
  音声フォールバックがない。次回は音声コマンドも検討。
improvement_items:     # CONDITIONAL時のみ
  - "音声入力によるナビゲーション追加"
```

## フェーズ5: 品質ゲート

### PASSフロー (≥7/10)
1. `findings.md` にレビュー結果・発見事項を追記
2. `REGISTRY.md` に完了済み機能を追記（ID、機能、APIカバレッジ、パターン参照、日付、スコア）
3. 再利用可能なパターンがあれば `patterns/` に抽出
4. `git add experiments/{category}/NNN-xxx/ REGISTRY.md patterns/` → commit → push
5. チケットの status を `passed` に更新

### CONDITIONALフロー (5-6/10)
1. `improvement_items` をBuilderにフィードバック
2. Builderが改修 → Tester再実行 → Reviewer再評価
3. **レビューリトライは最大2回**
4. 2回目でもCONDITIONAL以下 → FAIL扱いでarchive

### FAILフロー (≤4/10) または リトライ上限到達
1. `experiments/{category}/NNN-xxx/` を `archive/rejected/NNN-xxx/` に移動
2. `archive/rejected/NNN-xxx/rejection-reason.md` を生成（なぜ失敗したか）
3. `archive/failure-analysis.md` に失敗エントリを追記（ID、失敗フェーズ、理由、教訓）
4. チケットの status を `archived` に更新
5. 次のチケットに進む

## パターン抽出ルール

PASS した実験から以下のカテゴリでパターンを整理:

```
patterns/
├── ui-patterns.md           ← Glimmer UIの実装パターン
├── input-patterns.md        ← タッチパッド・音声入力パターン
├── architecture-patterns.md ← アプリ構造・ライフサイクルパターン
├── camera-patterns.md       ← カメラ・画像処理パターン
├── voice-patterns.md        ← TTS・Gemini Live統合パターン
└── ar-patterns.md           ← ARCore・Geospatialパターン
```

各パターンのフォーマット（AI-readable: AIがCLAUDE.mdとして読み込んでvibe codingに使える形式）:
```markdown
## パターン名

**いつ使う**: このパターンが必要な場面を1文で
**前提**: 必要な依存関係やインポート

\```kotlin
// コピペで動く完全なコード（importも含む）
// コメントは「なぜこうするか」を説明
\```

**ハマりポイント**:
- 注意点1
- 注意点2

**出典**: experiments/{category}/NNN-xxx
```

**重要**: パターンのコードスニペットは断片ではなく、コピペで動くレベルの完全性を維持する。
importを省略しない。コンテキストなしで読んでもわかるように書く。

## Orchestratorの判断ルール

1. `tickets/` から `status: queued` のチケットを `priority` 順にピック
2. `skills_needed` を確認し、Builderにどのスキルを読ませるか指示
3. Builder → Tester → Reviewer のシーケンスを管理
4. テスト5回失敗 or レビューリトライ2回で打ち切り判断
5. 完了後、次のチケットを自動でピック
6. 全チケット消化後、Plannerに次のチケット生成を依頼
