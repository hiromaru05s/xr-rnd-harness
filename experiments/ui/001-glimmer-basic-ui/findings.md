# 001: Glimmer基本UIコンポーネント動作確認 - 発見事項

## テスト結果
- 全静的チェック(T1-T7) PASS
- エミュレータ動作確認: UI描画OK、フォーカスナビゲーションOK、クラッシュなし

## レビュー結果
- スコア: 10/10 (PASS) — バグ修正後の再レビュー
- UX適合度: 3/3、コード品質: 3/3、再利用性: 2/2、ドキュメント: 2/2

## バグ修正履歴
### 差し戻し (2026-04-06): 初回起動後UIが表示されなくなる
**原因1**: GlassesMainActivityにLAUNCHER + requiredDisplayCategoryを同時指定。スマホ側MainActivityなし。
**修正**: MainActivity（LAUNCHER）を新規作成。ProjectedContext経由でGlassesMainActivity起動。

**原因2**: ProjectedDisplayController/ProjectedDeviceController未使用。
**修正**: FLAG_KEEP_SCREEN_ON設定、PresentationMode監視、CAPABILITY_VISUAL_UIチェックを追加。

**追加修正**: AGP 8.7.0→8.9.1（projected依存が8.9.1以上を要求）。isProjectedDeviceConnectedのAPI 36ガード。

## 発見事項

### Glimmerコンポーネントの動作特性
1. **GlimmerTheme**: `setContent { GlimmerTheme { ... } }` でラップするだけでフォーカスシステムが有効化される。追加設定不要。
2. **VerticalList + items**: `VerticalList { items(list) { ... } }` でリスト表示。LazyColumnは使わない。3アイテム以下がグラスの制約。
3. **フォーカスナビゲーション**: タッチパッドのDPAD_DOWN/UPでアウトラインベースのフォーカスが自動的に移動する。明示的なfocusRequester設定は不要。
4. **Card composable**: `title`、`action`、`content`の3スロットを持つ。Buttonをactionとcontentそれぞれに配置可能。
5. **Button**: `ButtonSize.Medium`と`ButtonSize.Large`を確認。`leadingIcon`スロットでアイコン付きボタンを構成。
6. **TitleChip**: `leadingIcon`スロット + contentラムダでステータスバー的な表示が可能。

### Projected APIの統合パターン
1. **2アクティビティ構成が必須**: MainActivity（LAUNCHER）→ ProjectedContext → GlassesMainActivity（xr_projected）。1アクティビティ構成は再起動時にUI消失バグを引き起こす。
2. **ProjectedDisplayController**: `FLAG_KEEP_SCREEN_ON`でグラスディスプレイのスヌーズを防止。`PresentationMode.VISUALS_ON`監視でビジュアルオフ時の適切な対応が可能。
3. **ProjectedDeviceController**: `CAPABILITY_VISUAL_UI`でディスプレイ有無を確認。ディスプレイなしデバイスでは音声ガイダンスモードにフォールバック可能。
4. **ライフサイクル管理**: DefaultLifecycleObserverでDisplayControllerのclose()を確実に実行。
5. **API 36要件**: `isProjectedDeviceConnected`はAPI 36以上が必要。`Build.VERSION.SDK_INT >= 36` + `@RequiresApi(36)`でガード。
6. **AGP要件**: projected:1.0.0-alpha05はAGP 8.9.1以上を要求。

### 黒背景 + 透過ディスプレイ
- `Color.Black` = 加算光方式で透明になる。UIコンテンツのみが見える。
- paddingは `horizontal=24.dp, vertical=16.dp` が適切なマージン。

## 次の実験への示唆
- 音声入力(Gemini Live)との組み合わせは未検証。次回voice系チケットで確認。
- タッチパッドのタップ(DPAD_CENTER)によるListItem選択も動作確認済み。
- Glimmerの`calculateContentColor`は今回未使用。動的テーマ切り替えが必要な場合に検討。
- ProjectedActivityCompatによるカメラボタン入力は未検証。次回input系チケットで確認。
