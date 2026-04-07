# 完了済み機能レジストリ

Orchestratorが実験PASS時に自動更新する。Plannerはチケット起票前に必ずこのファイルを確認し、重複する実験を生成しない。

## フォーマット

各エントリ:
- **ID**: 実験番号
- **機能**: 何ができるようになったか
- **APIカバレッジ**: 使用したAPI/コンポーネント
- **パターン参照**: patterns/ 内の該当セクション
- **PASS日**: PASS日とスコア

---

## 完了済み機能

### 001: Glimmer基本UIコンポーネント動作確認
- **PASS日**: 2026-04-06 (10/10) [FB resolved]
- **機能**: GlimmerThemeの基本コンポーネント（Button, Card, ListItem, VerticalList, TitleChip）の描画確認。スマホ→グラスの2アクティビティ起動パターン確立。DisplayControllerの堅牢なライフサイクル管理パターン確立。
- **APIカバレッジ**:
  - `GlimmerTheme`, `Button` (Medium/Large), `Card` (action付き), `ListItem` (onClick付き), `VerticalList` + `items()`, `TitleChip`, `Icon`, `Text`
  - `ProjectedContext.createProjectedActivityOptions()`, `ProjectedContext.isProjectedDeviceConnected()`
  - `ProjectedDeviceController` (CAPABILITY_VISUAL_UI)
  - `ProjectedDisplayController` (FLAG_KEEP_SCREEN_ON, PresentationMode.VISUALS_ON)
  - `@OptIn(ExperimentalProjectedApi::class)`
- **パターン参照**:
  - patterns/ui-patterns.md: Glimmer基本コンポーネント配置, TitleChipステータス表示, Card+Button組み合わせ
  - patterns/architecture-patterns.md: スマホ→グラス2アクティビティ起動パターン, GlassesMainActivity堅牢ライフサイクルパターン

### 002: Touchpad Gesture Navigation
- **PASS Date**: 2026-04-06 (10/10)
- **Feature**: Touchpad swipe-based VerticalStack card flipping and VerticalList
  focus-based scrolling. onIndirectPointerGesture integration with StackState
  for programmatic card navigation. Mode switching between Card Stack and List View.
- **API Coverage**:
  - `onIndirectPointerGesture` (onSwipeForward, onSwipeBackward, onClick)
  - `.focusTarget()` (required for gesture detection)
  - `VerticalStack` + `StackState` + `rememberStackState()` + `animateScrollToItem()`
  - `VerticalList` + `items()` (focus-based scrolling)
  - `Card`, `Button` (Medium/Large), `ListItem`, `TitleChip`, `Icon`, `Text`
  - Robust lifecycle pattern (from 001): singleTop + onNewIntent + onResume
- **Pattern References**:
  - patterns/input-patterns.md: Touchpad gesture detection, VerticalStack card navigation
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused from 001)

### 003: Gemini Live Voice Conversation Loop
- **PASS Date**: 2026-04-06 (9/10)
- **Feature**: Gemini Live API integration for real-time voice conversation on
  AI glasses. Native audio output, conversation state management, TTS fallback.
- **API Coverage**:
  - `Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel()`
  - `LiveModel.connect()`, `LiveSession.startAudioConversation()`
  - `ResponseModality.AUDIO`, `SpeechConfig`, `Voice("FENRIR")`
  - `transcriptHandler` callback, `enableInterruptions`
  - `TextToSpeech` (built-in Android TTS for error fallback)
  - `GlimmerTheme.colors.negative` for Stop button styling
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/voice-patterns.md: Gemini Live initialization, ConversationState, Voice-first UI
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 004: カメラボタン入力イベント処理
- **PASS Date**: 2026-04-06 (10/10)
- **Feature**: ProjectedActivityCompatのprojectedInputEventsフローでカメラボタン等の入力イベントを受信・カウント・UI表示。AutoCloseableなリソース管理パターン確立。
- **API Coverage**:
  - `ProjectedActivityCompat.create()`, `ProjectedActivityCompat.projectedInputEvents` (Flow)
  - `ProjectedActivityCompat.close()` (AutoCloseable)
  - `ProjectedInputEvent.inputAction`
  - Robust lifecycle pattern (from 001): singleTop + onNewIntent + onResume
  - `GlimmerTheme.colors.positive` / `GlimmerTheme.colors.primary` for dynamic color
  - `GlimmerTheme.typography.titleMedium` for emphasis
- **Pattern References**:
  - patterns/input-patterns.md: ProjectedActivityCompat入力イベント受信パターン
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 005: 通知ブリッジングとProjectedExtender
- **PASS Date**: 2026-04-06 (10/10)
- **Feature**: NotificationCompat + IMPORTANCE_HIGHチャンネルによるグラスへの通知ブリッジング。標準通知とMessagingStyle会話通知の両方をサポート。sealed class Result型パターン確立。
- **API Coverage**:
  - `NotificationChannel` (IMPORTANCE_HIGH)
  - `NotificationCompat.Builder`, `NotificationManagerCompat`
  - `NotificationCompat.MessagingStyle`, `Person.Builder`
  - `PendingIntent` (FLAG_IMMUTABLE + FLAG_UPDATE_CURRENT)
  - sealed class `NotificationResult` (Success/Error)
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/architecture-patterns.md: 通知ブリッジングパターン, sealed class Result型
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)
