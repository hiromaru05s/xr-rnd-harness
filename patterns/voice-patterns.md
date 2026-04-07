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
