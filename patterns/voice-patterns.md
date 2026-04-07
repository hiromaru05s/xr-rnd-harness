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
