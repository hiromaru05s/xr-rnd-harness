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

### 006: デバイスポーズトラッキング基礎
- **PASS Date**: 2026-04-06 (10/10)
- **Feature**: XR Runtime Session + ARCore ArDeviceによるデバイスポーズ(position+rotation)リアルタイム取得・表示。Session/Config/ArDeviceの基本パターン確立。
- **API Coverage**:
  - `Session.create()`, `SessionCreateSuccess`, `SessionCreateResult`
  - `Config(deviceTracking = DeviceTrackingMode.LAST_KNOWN)`
  - `Session.configure(config)`
  - `ArDevice.getInstance(session)`, `ArDevice.state` (StateFlow)
  - `Pose.translation` (Vector3), `Pose.rotation` (Quaternion), `Pose.Identity`
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/ar-patterns.md: XR Session初期化パターン, ArDeviceポーズ収集パターン
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 007: Glimmer DepthEffect/Surface高度活用
- **PASS Date**: 2026-04-06 (9/10)
- **Feature**: Glimmer Surface/SurfaceDefaults/Cardのカスタマイズパターン。カスタムボーダー、カスタムカラー、Modifier.surface()、Card全スロット活用。
- **API Coverage**:
  - `Modifier.surface(focusable, shape, color, border)`
  - `SurfaceDefaults.border(width, color)`
  - `BorderStroke` (Compose Foundation)
  - `GlimmerTheme.shapes.small` / `.medium` / `.large`
  - `GlimmerTheme.colors.surface` / `.primary` / `.positive`
  - `Card(title, subtitle, leadingIcon, color, border)` - 全スロット活用
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/ui-patterns.md: Surface/SurfaceDefaultsカスタマイズ, Card全スロット活用
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 008: TTS音声フィードバックシステム
- **PASS Date**: 2026-04-06 (10/10)
- **Feature**: Android TTS音声フィードバック管理。sealed class TtsState(5状態)、UtteranceProgressListener、PresentationMode連動のディスプレイON/OFF自動切替。
- **API Coverage**:
  - `TextToSpeech` (QUEUE_FLUSH, QUEUE_ADD)
  - `UtteranceProgressListener` (onStart, onDone, onError)
  - `PresentationMode.VISUALS_ON` 連動モード切替
  - sealed class `TtsState` (Initializing/Ready/Speaking/Completed/Error)
  - `GlimmerTheme.colors.negative` for stop button
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/voice-patterns.md: TtsManager sealed state管理パターン
  - patterns/architecture-patterns.md: PresentationMode連動パターン, Robust lifecycle (reused)

### 009: CameraXグラスカメラキャプチャ
- **PASS Date**: 2026-04-07 (10/10)
- **Feature**: CameraX via ProjectedDeviceContext for glasses outward-facing camera access. CaptureState sealed class (7 states) state machine. Permission handling via ProjectedPermissionsResultContract. ResolutionSelector with 640x480 for AI glasses optimized capture.
- **API Coverage**:
  - `ProjectedContext.createProjectedDeviceContext()` for glasses camera context
  - `ProcessCameraProvider.getInstance(projectedContext)` for camera provider
  - `ImageCapture.Builder()` with `ResolutionSelector`/`ResolutionStrategy`
  - `CameraSelector.DEFAULT_BACK_CAMERA` for outward-facing camera
  - `ProjectedPermissionsResultContract`, `ProjectedPermissionsRequestParams` (permissions sub-package)
  - `CaptureState` sealed class (Initializing, RequestingPermission, PermissionGranted, PermissionDenied, BindingCamera, Ready, Error)
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/camera-patterns.md: CameraX via ProjectedDeviceContext pattern
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 010: Geospatial位置情報取得
- **PASS Date**: 2026-04-07 (10/10)
- **Feature**: ARCore Geospatial API with VPS+GPS for geographic coordinates. Session creation with Config(geospatial=VPS_AND_GPS). ArDevice pose to GeospatialPose conversion. GeoState sealed class (6 states).
- **API Coverage**:
  - `Session.create()`, `SessionCreateSuccess`
  - `Config(geospatial = GeospatialMode.VPS_AND_GPS, deviceTracking = DeviceTrackingMode.LAST_KNOWN)`
  - `Geospatial.getInstance(session)`, `createGeospatialPoseFromPose(pose)`
  - `CreateGeospatialPoseFromPoseSuccess.pose` (GeospatialPose: latitude, longitude, altitude)
  - `ArDevice.getInstance(session)`, `ArDevice.state` (StateFlow)
  - `Pose.Identity` for tracking state detection
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/ar-patterns.md: Geospatial session initialization, Geospatial pose tracking
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 011: 音声+タッチパッド統合ナビゲーション
- **PASS Date**: 2026-04-07 (9/10)
- **Feature**: TTS voice feedback integrated with touchpad gesture navigation. Auto-read on card switch. PresentationMode-aware dual mode (Visual+Voice / Voice Only). UtteranceProgressListener for TTS state tracking.
- **API Coverage**:
  - `onIndirectPointerGesture` (onSwipeForward, onSwipeBackward, onClick) + `focusable()`
  - `TextToSpeech` (QUEUE_FLUSH), `UtteranceProgressListener` (onStart, onDone, onError)
  - `PresentationMode.VISUALS_ON` for dual-mode UI
  - TtsStatus enum (Idle, Speaking, Completed, Error)
  - Card-based navigation (3 items, FOV compliant)
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/input-patterns.md: TTS + touchpad integrated navigation
  - patterns/voice-patterns.md: TTS auto-read pattern
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 012: グラスハードウェア権限管理パターン
- **PASS Date**: 2026-04-07 (10/10)
- **Feature**: ProjectedPermissionsResultContract-based permission request flow. PermissionItemState enum (4 states). Progressive UI with state-dependent display. TTS fallback on denial. Retry capability.
- **API Coverage**:
  - `ProjectedPermissionsResultContract` (from `projected.permissions` package)
  - `ProjectedPermissionsRequestParams(permissions, rationale)`
  - `PermissionItemState` enum (NOT_REQUESTED, REQUESTING, GRANTED, DENIED)
  - CAMERA + RECORD_AUDIO multi-permission request
  - `TextToSpeech` for denial fallback
  - `checkExistingPermissions()` on onResume for state freshness
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/architecture-patterns.md: Glasses permission request flow, Robust lifecycle (reused)

### 013: Glimmerタイポグラフィ・カラーシステム活用
- **PASS Date**: 2026-04-07 (9/10)
- **Feature**: All 7 Glimmer typography styles and 8-color system demonstrated. Colors.copy() and Typography.copy() for custom theming. Touchpad-driven 3-page navigation.
- **API Coverage**:
  - All 7 typography styles: `titleLarge`, `titleMedium`, `titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `caption`
  - All 8 colors: `primary`, `secondary`, `positive`, `negative`, `background`, `surface`, `outline`, `outlineVariant`
  - `Colors.copy(primary = ...)` for custom color theme
  - `Typography.copy(titleLarge = ...)` for custom font weight
  - Nested `GlimmerTheme(colors, typography)` for scoped theming
  - `onIndirectPointerGesture` + `focusable()` for page navigation
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/ui-patterns.md: Typography scale, Colors.copy() custom theme, Nested GlimmerTheme
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 014: Gemini Live Function Calling統合
- **PASS Date**: 2026-04-07 (10/10)
- **Feature**: Gemini Live Function Calling for voice-controlled shopping list management. FunctionDeclaration/Tool/FunctionCallPart/FunctionResponsePart integration. Agent-style UI with mutableStateListOf.
- **API Coverage**:
  - `FunctionDeclaration(name, description, parameters)` with `Schema.string()`
  - `Tool.functionDeclarations(listOf(...))` for tool registration
  - `LiveSession.startAudioConversation(functionCallHandler)` with handler
  - `FunctionCallPart.name`, `FunctionCallPart.args` (Map<String, JsonElement>?)
  - `FunctionResponsePart(name, JsonObject)` for response
  - `JsonObject`, `JsonPrimitive` (kotlinx.serialization.json)
  - Firebase AI PublicPreviewAPI opt-in
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/voice-patterns.md: Gemini Function Calling handler pattern
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)

### 015: HostDeviceContextによるスマホハードウェアアクセス
- **PASS Date**: 2026-04-07 (10/10)
- **Feature**: ProjectedContext.createHostDeviceContext() for cross-device phone hardware access from glasses. Context type detection and device identification.
- **API Coverage**:
  - `ProjectedContext.createHostDeviceContext(activity)` for phone context
  - `ProjectedContext.isProjectedDeviceContext(context)` for type detection
  - `ProjectedContext.getProjectedDeviceName(context)` for device name
  - `Vibrator` via host context for phone vibration
  - `ContextInfo` data class for state management
  - Robust lifecycle pattern (from 001)
- **Pattern References**:
  - patterns/architecture-patterns.md: HostDeviceContext cross-device pattern, Robust lifecycle (reused)

### 016: Camera + Gemini Live Visual QA
- **PASS Date**: 2026-04-06 (10/10)
- **Feature**: CameraX image capture + Gemini Live multimodal visual Q&A integration. Camera button triggers capture, image sent to Gemini as InlineData, AI analyzes and responds via audio. Continuous voice conversation about captured images.
- **API Coverage**:
  - `CameraX` via `ProjectedContext.createProjectedDeviceContext()` for glasses camera
  - `ImageCapture.OnImageCapturedCallback`, `ImageProxy` to Base64 conversion
  - `Firebase.ai().liveModel()` with `ResponseModality.AUDIO`
  - `LiveSession.send(content { inlineData() })` for multimodal image+text
  - `LiveSession.startAudioConversation(transcriptHandler)` for voice conversation
  - `ProjectedActivityCompat.projectedInputEvents` for camera button trigger
  - `AppState` sealed class (6 states: Initializing/Ready/Capturing/Analyzing/Conversing/Error)
  - Robust lifecycle pattern (singleTop + onNewIntent + onResume)
- **Pattern References**:
  - patterns/camera-patterns.md: CameraX + Gemini multimodal capture pattern
  - patterns/architecture-patterns.md: Multi-system state machine pattern

### 017: Notification Voice Bridge
- **PASS Date**: 2026-04-06 (9/10)
- **Feature**: Notification-to-voice bridge with rolling 3-item queue, auto TTS read-aloud on arrival, touchpad swipe navigation (forward/backward/dismiss). PresentationMode-aware dual mode support.
- **API Coverage**:
  - `TextToSpeech` (QUEUE_FLUSH) + `UtteranceProgressListener` for TTS state
  - `onIndirectPointerGesture` (onSwipeForward/onSwipeBackward/onClick) + `focusTarget()`
  - `NotificationQueueManager` rolling queue (max 3 items, newest first)
  - `BridgeState` sealed class (5 states: Initializing/Waiting/Showing/Reading/Error)
  - `TtsStatus` enum (Idle/Speaking/Completed/ErrorOccurred)
  - Card with subtitle for position indicator, negative color Dismiss button
  - Robust lifecycle pattern (singleTop + onNewIntent + onResume)
- **Pattern References**:
  - patterns/input-patterns.md: Notification queue + touchpad navigation
  - patterns/voice-patterns.md: Auto-read notification with TTS
  - patterns/architecture-patterns.md: Robust lifecycle pattern (reused)
