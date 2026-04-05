---
name: reject-experiment
description: >
  Hiromaruが動作確認後に実験を差し戻すコマンド。PASS済みの実験でも
  人間の目で見て微妙だったものを理由付きでrejectできる。
  「この実験微妙」「やり直し」「戻して」等の指示で使う。
---

# 実験の人間レビュー差し戻し

Hiromaruが実機やエミュレータで動作確認した結果、微妙だったものを差し戻すフロー。

## 使い方

Claude Codeで以下のように指示する:

```
実験001を差し戻し。理由: フォーカスのアウトラインが細すぎて透過ディスプレイ上で見えない。もっと太くするか光らせるかしないとダメ
```

```
experiments/ui/002をreject。タッチパッドのスワイプ感度が悪くて3回に1回しか反応しない
```

## 差し戻し時にやること

1. **human-feedback.yaml を生成**:
   `experiments/{category}/NNN-xxx/human-feedback.yaml` に保存

```yaml
experiment_id: "NNN"
reviewer: "human"
timestamp: "ISO8601"
verdict: "REJECTED"
reason: |
  （Hiromaruが述べた理由をそのまま記録）
specific_issues:
  - "具体的な問題点1"
  - "具体的な問題点2"
improvement_direction: |
  （理由から推測される改善方向）
```

2. **REGISTRY.md から該当エントリを削除**（PASSで登録済みの場合）

3. **patterns/ から該当パターンを削除**（抽出済みの場合）

4. **human-rejections.md に追記**:
   `archive/human-rejections.md` に以下を追記:

```markdown
### NNN: 実験タイトル
- **差し戻し日**: 2026-04-05
- **元のスコア**: 8/10 PASS
- **差し戻し理由**: （理由）
- **具体的問題**: （問題リスト）
- **教訓**: （この失敗から何を学ぶか。Planner/Builder両方が参照）
```

5. **チケットのステータスを更新**:
   - `status: passed` → `status: queued` に戻す（再実行対象）
   - `retry_count` はリセットしない（累計を保持）
   - `human_feedback: true` フラグを追加

6. **git操作**:
```bash
git add experiments/{category}/NNN-xxx/human-feedback.yaml
git add archive/human-rejections.md REGISTRY.md patterns/
git commit -m "experiment NNN: human rejection - 理由の要約"
git push
```

## 再実行時の特別ルール

`human_feedback: true` がついたチケットを再処理するとき:
- Builderは `human-feedback.yaml` の `reason` と `specific_issues` を**必ず読む**
- 同じ問題を繰り返さないように、具体的に何を変えるかをREADMEに記載
- Reviewerは再レビュー時に `human-feedback.yaml` の指摘が解消されているか重点確認

## Planner/Builderへの波及

- **Planner**: `archive/human-rejections.md` を REGISTRY.md と同様に起票前に確認。
  人間が「これは微妙」と言ったパターンと類似する実験を安易に起票しない。
  ただし、人間のフィードバックを踏まえた改善アプローチなら起票OK。

- **Builder**: `human_feedback: true` のチケットは、通常チケットより注意深く実装。
  `human-feedback.yaml` を読んで、何が問題だったかを完全に理解してから着手。
