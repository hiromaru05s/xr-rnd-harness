# 001: Glimmer基本UIコンポーネント動作確認 - 発見事項

## テスト結果
- 全静的チェック(T1-T7) PASS
- エミュレータ動作確認: UI描画OK、フォーカスナビゲーションOK、クラッシュなし

## レビュー結果
- スコア: 9/10 (PASS)
- UX適合度: 3/3、コード品質: 3/3、再利用性: 2/2、ドキュメント: 1/2

## 発見事項

### Glimmerコンポーネントの動作特性
1. **GlimmerTheme**: `setContent { GlimmerTheme { ... } }` でラップするだけでフォーカスシステムが有効化される。追加設定不要。
2. **VerticalList + items**: `VerticalList { items(list) { ... } }` でリスト表示。LazyColumnは使わない。3アイテム以下がグラスの制約。
3. **フォーカスナビゲーション**: タッチパッドのDPAD_DOWN/UPでアウトラインベースのフォーカスが自動的に移動する。明示的なfocusRequester設定は不要。
4. **Card composable**: `title`、`action`、`content`の3スロットを持つ。Buttonをactionとcontentそれぞれに配置可能。
5. **Button**: `ButtonSize.Medium`と`ButtonSize.Large`を確認。`leadingIcon`スロットでアイコン付きボタンを構成。
6. **TitleChip**: `leadingIcon`スロット + contentラムダでステータスバー的な表示が可能。

### 黒背景 + 透過ディスプレイ
- `Color.Black` = 加算光方式で透明になる。UIコンテンツのみが見える。
- paddingは `horizontal=24.dp, vertical=16.dp` が適切なマージン。

### AndroidManifest
- `android:requiredDisplayCategory="xr_projected"` がグラス投影に必須。
- `Theme.Black.NoTitleBar.Fullscreen` で全画面黒背景を確保。

## 次の実験への示唆
- 音声入力(Gemini Live)との組み合わせは未検証。次回voice系チケットで確認。
- タッチパッドのタップ(DPAD_CENTER)によるListItem選択も動作確認済み。
- Glimmerの`calculateContentColor`は今回未使用。動的テーマ切り替えが必要な場合に検討。
