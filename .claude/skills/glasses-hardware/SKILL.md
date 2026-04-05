---
name: glasses-hardware
description: AIグラスのハードウェアアクセスAPIリファレンス。TTS音声出力、Gemini Live API統合、CameraXによるグラスカメラアクセス、通知ブリッジング、Bluetooth音声、投影コンテキストからのハードウェアアクセスに関するタスクでは必ずこのスキルを使う。
---

# AIグラス ハードウェア・音声・AI統合リファレンス

TTS、Gemini Live、カメラ、通知、ハードウェアアクセスの包括的リファレンス。

## 音声I/O: TextToSpeech (TTS)

TTS はAndroid組み込み（追加ライブラリ不要）、オフライン動作可能。ディスプレイレスモードでのエラー通知に最適。

### インスタンス化（onCreate内）
```kotlin
private var tts: TextToSpeech? = null

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    tts = TextToSpeech(this) { status ->
        if (status == TextToSpeech.SUCCESS) { /* 初期化成功 */ }
    }
}
```

### 読み上げ
```kotlin
override fun onStart() {
    super.onStart()
    tts?.speak("Welcome to Android XR Glasses!",
        TextToSpeech.QUEUE_FLUSH, null, "welcome_utterance")
}
```

### 中断・クリーンアップ
```kotlin
tts?.stop()       // 現在の発話を中断
tts?.shutdown()   // onDestroy()で呼び出し、リソース解放
```

**QUEUE_FLUSH**: 即座に読み上げ、他の発話を中断。`utteranceId`で読み上げ完了タイミングを特定（`UtteranceProgressListener`）。

---

## Gemini Live API統合（Firebase AI Logic経由）

Gemini Live APIは音声入出力をシームレスに処理する会話型インターフェース。エージェントエクスペリエンスの構築にも使用可能。

**注意**: 永続的なインターネット接続が必要、費用発生、プロジェクトあたりの同時接続数に制限あり。ディスプレイレスAIグラスではエラー処理にTTSを併用すること。

### 依存関係
```kotlin
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-ai")
}
```

### LiveModel初期化
```kotlin
val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
    modelName = "gemini-2.5-flash-native-audio-preview-12-2025",
    generationConfig = liveGenerationConfig {
        responseModality = ResponseModality.AUDIO
        speechConfig = SpeechConfig(voice = Voice("FENRIR"))
    },
    systemInstruction = content {
        text("You are a helpful assistant for AI glasses users...")
    },
    tools = listOf(/* FunctionDeclaration tools */)
)
```

### セッション開始・音声会話
```kotlin
val session = model.connect()
session.startAudioConversation(::functionCallHandler)
```

### 関数呼び出し（Function Calling）
```kotlin
// 関数定義
val addListFunctionDeclaration = FunctionDeclaration(
    name = "addList",
    description = "Function adding an item to the list",
    parameters = mapOf(
        "item" to Schema.string("A short string describing the item")
    )
)
val addListTool = Tool.functionDeclarations(listOf(addListFunctionDeclaration))

// ハンドラ
fun functionCallHandler(functionCall: FunctionCallPart): FunctionResponsePart {
    return when (functionCall.name) {
        "addList" -> {
            val itemName = functionCall.args["item"]!!.jsonPrimitive.content
            addList(itemName)
            FunctionResponsePart(functionCall.name,
                JsonObject(mapOf(
                    "success" to JsonPrimitive(true),
                    "message" to JsonPrimitive("Item $itemName added")
                )))
        }
        else -> FunctionResponsePart(functionCall.name,
            JsonObject(mapOf("error" to JsonPrimitive("Unknown function"))))
    }
}
```

### LiveSession主要API
- `session.startAudioConversation(functionCallHandler?, transcriptHandler?, enableInterruptions?)` — 音声会話開始
- `session.stopAudioConversation()` — 音声会話停止
- `session.send(text: String)` / `session.send(content: Content)` — テキスト/コンテンツ送信
- `session.sendAudioRealtime(audio: InlineData)` — 音声データ送信（16bit PCM, 24kHz推奨）
- `session.sendVideoRealtime(video: InlineData)` — 動画フレーム送信
- `session.close()` — セッション終了
- `session.receive(): Flow<LiveServerMessage>` — レスポンス受信

### 対応モデル
- Gemini Developer API: `gemini-2.5-flash-native-audio-preview-12-2025`
- Vertex AI: `gemini-live-2.5-flash-native-audio`

---

## ハードウェアアクセス（投影コンテキスト）

### グラスのハードウェアにアクセスする

グラス用アクティビティ内では、アクティビティコンテキスト自体が既に投影されたコンテキスト。追加操作不要。

グラスアクティビティ外（電話アクティビティやサービス）からグラスのハードウェアにアクセスする場合:
```kotlin
@OptIn(ExperimentalProjectedApi::class)
private fun getGlassesContext(context: Context): Context? {
    return try {
        ProjectedContext.createProjectedDeviceContext(context)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Failed to create projected device context", e)
        null
    }
}
```

`ProjectedContext.isProjectedDeviceConnected`が`true`の間、投影コンテキストは有効。切断時はリソースクリーンアップ必須。

### スマートフォンのハードウェアにグラスからアクセスする
```kotlin
@OptIn(ExperimentalProjectedApi::class)
private fun getPhoneContext(activity: ComponentActivity): Context? {
    return try {
        ProjectedContext.createHostDeviceContext(activity)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "Failed to create host device context", e)
        null
    }
}
```

**注意**: `getApplicationContext()`は使わないこと。グラスアクティビティが最後のコンポーネントだった場合、アプリケーションコンテキストがグラスのコンテキストを誤って返す可能性がある。

### Bluetooth音声アクセス
AIグラスは標準Bluetoothオーディオデバイスとして接続（ヘッドセット + A2DPプロファイル）。音声I/Oアプリはグラス専用でなくても動作する。`RECORD_AUDIO`権限はスマートフォン側で制御。

---

## CameraXによるグラスカメラアクセス

### 画像キャプチャ
```kotlin
private fun startCameraOnGlasses(activity: ComponentActivity) {
    val projectedContext = try {
        ProjectedContext.createProjectedDeviceContext(activity)
    } catch (e: IllegalStateException) {
        Log.e(TAG, "AI Glasses context could not be created", e)
        return
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(projectedContext)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA  // グラスの外向きカメラ

        if (!cameraProvider.hasCamera(cameraSelector)) return@addListener

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(
                Size(1920, 1080),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
            )).build()

        val imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(activity, cameraSelector, imageCapture)
    }, ContextCompat.getMainExecutor(activity))
}
```

### 推奨解像度・フレームレート

| ユースケース | 解像度 | フレームレート |
|-------------|--------|--------------|
| ビデオ通信 | 1280×720 | 15 FPS |
| コンピュータビジョン | 640×480 | 10 FPS |
| AI動画ストリーミング | 640×480 | 1 FPS |

**重要**: AIグラスはバッテリーと放熱が制限されている。解像度/FPSは電力と温度に大きく影響する。

---

## 通知ブリッジング

### 基本
AIグラスは標準Android通知フレームワークを使用。`NotificationCompat` APIで実装し、Androidがデバイス能力に基づき表示を調整。

### サポートされる通知スタイル
- 標準スタイル（`NotificationCompat.Style`）
- `MessagingStyle` — ダイレクト返信対応、音声返信/スマートリプライ
- `BigPictureStyle`
- `ProgressStyle`
- `MediaStyle`
- `CallStyle`（ライブアップデート条件を満たす場合のみ）

**非サポート**: `RemoteViews`カスタム通知はブリッジされない。

### ブリッジング条件
1. `IMPORTANCE_HIGH` または `IMPORTANCE_MAX` チャンネル
2. 通知タイトルが非null・非空
3. `FLAG_LOCAL_ONLY`が付いていない
4. 継続的通知でない（ライブアップデート条件を満たす場合を除く）
5. ユーザーがグラスアプリで当該アプリの通知を有効にしている

### ProjectedExtender（グラス固有の通知動作）
```kotlin
val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
    setSmallIcon(R.drawable.ic_notification)
    setContentTitle("Navigation in Progress")
    setContentText("Tap to see details")
    setContentIntent(phonePendingIntent)  // スマートフォン用

    // グラス固有のインテント
    val projectedExtender = ProjectedExtender().setContentIntent(glassesPendingIntent)
    extend(projectedExtender)
}
notificationManager.notify(NOTIFICATION_ID, builder.build())
```

---

