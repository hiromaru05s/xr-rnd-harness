# Voice Patterns

> AI-readable patterns for TTS, Gemini Live, and voice conversation on AI glasses.
> Code snippets are copy-paste ready with full imports.

---

## Gemini Live API Initialization (Firebase AI Logic)

**When to use**: Setting up Gemini Live for voice conversation on AI glasses
**Prerequisites**: `implementation(platform("com.google.firebase:firebase-bom:34.11.0"))`, `implementation("com.google.firebase:firebase-ai")`, google-services.json

```kotlin
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig

// Initialize Gemini Live model with native audio
val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
    modelName = "gemini-2.5-flash-native-audio-preview-12-2025",
    generationConfig = liveGenerationConfig {
        responseModality = ResponseModality.AUDIO
        speechConfig = SpeechConfig(voice = Voice("FENRIR"))
    },
    systemInstruction = content {
        text("You are a helpful assistant for AI glasses users. Keep responses brief.")
    },
)

// Connect and start audio conversation
val session = model.connect()
session.startAudioConversation(
    transcriptHandler = { transcript -> /* update UI */ },
    enableInterruptions = true,
)

// When done:
session.stopAudioConversation()
session.close()
```

**Gotchas**:
- Requires active internet connection
- google-services.json must be in app/ directory
- RECORD_AUDIO permission required for microphone access
- ResponseModality.AUDIO enables native speech output from Gemini
- Always close session when conversation ends to avoid resource leaks
- Model name format: "gemini-2.5-flash-native-audio-preview-12-2025"

**Source**: experiments/voice/003-gemini-voice-loop

---

## Voice-First UI with Glimmer

**When to use**: Displaying conversation state on AI glasses transparent display
**Prerequisites**: `implementation("androidx.xr.glimmer:glimmer:1.0.0-alpha08")`

```kotlin
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.ButtonSize
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TitleChip

// Minimal voice UI: state indicator + transcript card
// TitleChip for at-a-glance state
TitleChip(
    leadingIcon = { Icon(Icons.Default.Mic, null) }
) {
    Text("Listening...")
}

// Card for transcript/prompt with action button
Card(
    title = { Text("You said") },
    action = {
        Button(
            onClick = { /* stop conversation */ },
            buttonSize = ButtonSize.Medium,
            color = GlimmerTheme.colors.negative, // Red for stop action
            leadingIcon = { Icon(Icons.Default.MicOff, null) },
        ) { Text("Stop") }
    }
) {
    Text("Last transcript text here")
}
```

**Gotchas**:
- Voice UI should be minimal: state indicator + one card max
- Use GlimmerTheme.colors.negative for destructive actions (Stop)
- TitleChip icon should change based on state (Mic/MicOff/Info)
- No list needed for voice UI; transcript is a single text block

**Source**: experiments/voice/003-gemini-voice-loop

---

## TTS Fallback for Error Notification

**When to use**: Notifying user of errors via speech when display is not sufficient
**Prerequisites**: Built-in Android API, no additional dependencies

```kotlin
import android.speech.tts.TextToSpeech

// Initialize in onCreate
private var tts: TextToSpeech? = null
tts = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) { /* ready */ }
}

// Speak error message
tts?.speak("Connection failed. Please try again.",
    TextToSpeech.QUEUE_FLUSH, null, "error_id")

// Cleanup in onDestroy
tts?.shutdown()
```

**Gotchas**:
- QUEUE_FLUSH interrupts any current speech
- Always shutdown in onDestroy to release resources
- TTS works offline - good for error fallback when Gemini is unreachable
- utteranceId parameter can be used with UtteranceProgressListener

**Source**: experiments/voice/003-gemini-voice-loop

---

## TtsManager Sealed State管理パターン

**いつ使う**: AIグラスでTTS音声フィードバックを管理するとき
**前提**: Android標準API（追加ライブラリ不要）

```kotlin
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

class TtsManager(
    context: Context,
    private val onStateChanged: (TtsState) -> Unit,
) {
    sealed class TtsState {
        data object Initializing : TtsState()
        data object Ready : TtsState()
        data class Speaking(val utteranceId: String) : TtsState()
        data class Completed(val utteranceId: String) : TtsState()
        data class Error(val message: String) : TtsState()
    }

    private var tts: TextToSpeech? = null
    private var utteranceCounter = 0

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) { onStateChanged(TtsState.Speaking(utteranceId)) }
                    override fun onDone(utteranceId: String) { onStateChanged(TtsState.Completed(utteranceId)) }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) { onStateChanged(TtsState.Error(utteranceId)) }
                })
                onStateChanged(TtsState.Ready)
            }
        }
    }

    fun speakFlush(text: String): String? {
        val id = "utt_${++utteranceCounter}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        return id
    }

    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }
}
```

**ハマりポイント**:
- QUEUE_FLUSH: 即座に読み上げ、他を中断。QUEUE_ADD: キューに追加
- UtteranceProgressListenerのonErrorは@Deprecated。新しいオーバーロードもあるが後方互換のため旧版も実装
- shutdown()はonDestroyで必ず呼ぶ。リソースリーク防止
- runOnUiThread{}でUI更新（Listenerコールバックは非メインスレッド）

**出典**: experiments/voice/008-tts-audio-feedback

---

## Gemini Function Calling Handler Pattern

**When to use**: When implementing voice-driven app actions via Gemini Live Function Calling
**Prerequisites**: `implementation(platform("com.google.firebase:firebase-bom:34.11.0"))`, `implementation("com.google.firebase:firebase-ai")`, `implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")`

```kotlin
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// 1. Declare functions
val addItemDecl = FunctionDeclaration(
    name = "addItem",
    description = "Add an item to the list",
    parameters = mapOf("item" to Schema.string("The item name"))
)

// 2. Create tool
val tools = listOf(Tool.functionDeclarations(listOf(addItemDecl)))

// 3. Handler function
fun handleFunctionCall(call: FunctionCallPart): FunctionResponsePart {
    return when (call.name) {
        "addItem" -> {
            val name = call.args?.get("item")?.toString()?.trim('"') ?: "unknown"
            // Do the action...
            FunctionResponsePart(call.name, JsonObject(mapOf(
                "success" to JsonPrimitive(true),
                "message" to JsonPrimitive("Added " + name)
            )))
        }
        else -> FunctionResponsePart(call.name, JsonObject(mapOf(
            "error" to JsonPrimitive("Unknown function")
        )))
    }
}

// 4. Start conversation with handler
session.startAudioConversation(functionCallHandler = ::handleFunctionCall)
```

**Gotchas**:
- kotlinx-serialization-json dependency required (not included in Firebase BOM)
- kotlin.plugin.serialization plugin required in build.gradle.kts
- FunctionCallPart.args is nullable Map<String, JsonElement>?
- FunctionResponsePart name MUST match the FunctionCallPart.name
- Firebase AI preview APIs require compiler opt-in: `-opt-in=com.google.firebase.ai.type.PublicPreviewAPI`
- String values in args include quotes - use `.trim('"')` to clean

**Source**: experiments/voice/014-gemini-function-calling

---

## Auto-Read Notification with TTS

**When to use**: When automatically reading notifications aloud on AI glasses with touchpad control
**Prerequisites**: Android TTS (built-in, no additional dependencies)

```kotlin
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

enum class TtsStatus { Idle, Speaking, Completed, ErrorOccurred }

// In Activity:
private var tts: TextToSpeech? = null
private var ttsStatus by mutableStateOf(TtsStatus.Idle)

// Initialize with UtteranceProgressListener for state tracking
tts = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uid: String) { runOnUiThread { ttsStatus = TtsStatus.Speaking } }
            override fun onDone(uid: String) { runOnUiThread { ttsStatus = TtsStatus.Completed } }
            @Deprecated("Deprecated in Java")
            override fun onError(uid: String) { runOnUiThread { ttsStatus = TtsStatus.ErrorOccurred } }
        })
    }
}

// Auto-read on notification arrival
fun onNotificationReceived(item: NotificationItem) {
    queueManager.addNotification(item)
    speakNotification(item)
}

fun speakNotification(item: NotificationItem) {
    val text = item.appName + ". " + item.title + ". " + item.text
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notif_" + item.id)
}

// Navigation also triggers re-read
fun navigateForward() {
    val next = queueManager.moveToNext()
    if (next != null) speakNotification(next)
}
```

**Gotchas**:
- QUEUE_FLUSH interrupts previous notification read for immediate new one
- UtteranceProgressListener callbacks run on non-main thread - use runOnUiThread
- Format: "AppName. Title. Text" with period separators for natural TTS pacing
- Use unique utterance IDs (notif_$id) for tracking specific notifications
- On dismiss with empty queue: tts?.stop() and reset status to Idle
- Clean up in onDestroy: tts?.stop(); tts?.shutdown()

**Source**: experiments/integration/017-notification-voice-bridge

---
