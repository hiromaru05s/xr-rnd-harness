# 001: Glimmer基本UIコンポーネント動作確認 - 発見事項

## テスト結果
- 全静的チェック(T1-T7) PASS
- エミュレータ動作確認: 静的チェックのみ（Gradle環境なし）

## レビュー結果
- スコア: 10/10 (PASS) — フィードバック対応後の再レビュー
- UX適合度: 3/3、コード品質: 3/3、再利用性: 2/2、ドキュメント: 2/2

## 人間フィードバック対応結果
- UI再表示不可バグ: RESOLVED
- テキスト中央ずれ: RESOLVED
- improvement_direction_followed: true

## バグ修正履歴

### 差し戻し #1 (2026-04-06): Manifest/ディスプレイ制御バグ
**原因1**: GlassesMainActivityにLAUNCHER + requiredDisplayCategoryを同時指定。スマホ側MainActivityなし。
**修正**: MainActivity（LAUNCHER）を新規作成。ProjectedContext経由でGlassesMainActivity起動。

**原因2**: ProjectedDisplayController/ProjectedDeviceController未使用。
**修正**: FLAG_KEEP_SCREEN_ON設定、PresentationMode監視、CAPABILITY_VISUAL_UIチェックを追加。

### 差し戻し #2 (2026-04-06 20:13:40): UI再表示不可 + テキスト中央ずれ
**原因1**: ライフサイクル遷移時にDisplayControllerが再初期化されない。onCreateのみでの初期化が不十分。
**修正**: singleTop launchMode + onNewIntent再初期化 + onResume復帰チェック + FLAG_ACTIVITY_CLEAR_TOP/SINGLE_TOPの多層防御。

**原因2**: VerticalListのデフォルトhorizontalAlignmentがAlignment.Start。BoxのcontentAlignmentがTopCenter。
**修正**: VerticalListにhorizontalAlignment = CenterHorizontally明示。BoxのcontentAlignmentをCenterに変更。CardにfillMaxWidth追加。

## 発見事項

### Glimmerコンポーネントの動作特性
1. **GlimmerTheme**: `setContent { GlimmerTheme { ... } }` でラップするだけでフォーカスシステムが有効化される。追加設定不要。
2. **VerticalList + items**: `VerticalList { items(list) { ... } }` でリスト表示。LazyColumnは使わない。3アイテム以下がグラスの制約。
3. **VerticalListの中央揃え**: デフォルトはAlignment.Start。中央揃えにするには `horizontalAlignment = Alignment.CenterHorizontally` を明示する必要がある。
4. **フォーカスナビゲーション**: タッチパッドのDPAD_DOWN/UPでアウトラインベースのフォーカスが自動的に移動する。明示的なfocusRequester設定は不要。
5. **Card composable**: `title`、`action`、`content`の3スロットを持つ。Buttonをactionとcontentそれぞれに配置可能。
6. **Button**: `ButtonSize.Medium`と`ButtonSize.Large`を確認。`leadingIcon`スロットでアイコン付きボタンを構成。
7. **TitleChip**: `leadingIcon`スロット + contentラムダでステータスバー的な表示が可能。

### Projected APIの統合パターン
1. **2アクティビティ構成が必須**: MainActivity（LAUNCHER）→ ProjectedContext → GlassesMainActivity（xr_projected）。1アクティビティ構成は再起動時にUI消失バグを引き起こす。
2. **singleTopモードが推奨**: GlassesMainActivityにlaunchMode="singleTop"を設定し、onNewIntentでDisplayControllerを再初期化する。これにより別画面から戻った際のUI再表示問題を防げる。
3. **ライフサイクル管理の多層防御**: onDestroy(LifecycleObserver) + onNewIntent(singleTop) + onResume(nullチェック) + onStop(isFinishing)の4点でDisplayControllerを管理する。
4. **ProjectedDisplayController**: `FLAG_KEEP_SCREEN_ON`でグラスディスプレイのスヌーズを防止。`PresentationMode.VISUALS_ON`監視でビジュアルオフ時の適切な対応が可能。
5. **FLAG_ACTIVITY_CLEAR_TOP + FLAG_ACTIVITY_SINGLE_TOP**: MainActivityからの起動時にこのフラグを付けることで、既存インスタンスの再利用を強制する。
6. **API 36要件**: `isProjectedDeviceConnected`はAPI 36以上が必要。`Build.VERSION.SDK_INT >= 36` + `@RequiresApi(36)`でガード。

### 黒背景 + 透過ディスプレイ
- `Color.Black` = 加算光方式で透明になる。UIコンテンツのみが見える。
- paddingは `horizontal=24.dp, vertical=16.dp` が適切なマージン。

## 次の実験への示唆
- 音声入力(Gemini Live)との組み合わせは未検証。次回voice系チケットで確認。
- タッチパッドのタップ(DPAD_CENTER)によるListItem選択も動作確認済み。
- Glimmerの`calculateContentColor`は今回未使用。動的テーマ切り替えが必要な場合に検討。
- ProjectedActivityCompatによるカメラボタン入力は未検証。次回input系チケットで確認。
- **重要**: GlassesMainActivityの堅牢なライフサイクル管理パターンは、以降の全実験で必ず踏襲すること。
