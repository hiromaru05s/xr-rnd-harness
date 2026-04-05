# 失敗分析レジストリ

archiveされた実験の失敗パターンを集約する。
Orchestratorがarchive時に自動追記。Plannerはチケット起票前にこれを確認し、同じ失敗を繰り返さない。

## フォーマット

各エントリ:
- **ID**: 実験番号
- **失敗フェーズ**: test / review
- **失敗理由**: 何がなぜダメだったか
- **教訓**: 次回同じ領域に挑戦する際のアドバイス

---

## 失敗履歴

（まだなし。実験がarchiveされるごとにOrchestratorが追記する）

<!-- 記入例:
### 005: CameraXリアルタイムプレビュー
- **失敗フェーズ**: test（ビルド失敗×5）
- **失敗理由**: CameraXのグラスカメラアクセスにProjectedContextが必要だが、依存関係の解決ができなかった
- **教訓**: CameraX使用時はProjected APIの初期化順序を先に確認。glasses-hardwareスキルのCameraXセクションを必ず参照
-->
