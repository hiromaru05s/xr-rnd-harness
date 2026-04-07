# 003: Gemini Live Voice Conversation Loop

## Hypothesis
Firebase AI Logic via Gemini Live API can implement a real-time voice
conversation loop: glasses mic input -> Gemini processing -> TTS response.

## Technology Used
- Skills: glasses-hardware, glimmer-api, glasses-arch
- Libraries:
  - com.google.firebase:firebase-ai (via firebase-bom:34.11.0)
  - androidx.xr.glimmer:glimmer:1.0.0-alpha08
  - androidx.xr.projected:projected:1.0.0-alpha05
  - android.speech.tts.TextToSpeech (built-in)
- Model: gemini-2.5-flash-native-audio-preview-12-2025
- Compose BOM: 2025.01.00
- Kotlin + Compose + Gradle Kotlin DSL (compileSdk=36, minSdk=35)

## Implementation
### Voice Conversation Flow
1. User taps "Start" button on glasses UI
2. App connects to Gemini Live API via Firebase AI Logic
3. Gemini Live session starts with native audio modality
4. User speaks -> audio streamed to Gemini via startAudioConversation()
5. Gemini responds with audio (native audio output)
6. Transcript displayed on glasses UI via transcriptHandler callback
7. User taps "Stop" to end session

### Conversation States (minimal glance-able UI)
- IDLE: Ready to start
- CONNECTING: Establishing Gemini Live connection
- LISTENING: Mic active, waiting for user speech
- THINKING: Processing user input
- SPEAKING: Gemini responding
- ERROR: Connection or API error (TTS fallback)

### Key Design Decisions
- **Native audio output**: Using ResponseModality.AUDIO for Gemini to generate
  speech directly, rather than text->TTS pipeline
- **TTS as fallback**: Built-in TTS used only for error notifications
- **Minimal UI**: Only state indicator + transcript card. No complex controls.
- **Interruption support**: enableInterruptions=true for natural conversation
- **Voice selection**: FENRIR voice for clear, natural speech

### Architecture
- MainActivity (phone) -> ProjectedContext -> GlassesMainActivity (glasses)
- GlassesMainActivity manages Gemini session lifecycle
- VoiceLoopScreen is a stateless Composable receiving state from Activity
- Robust lifecycle pattern from experiment 001 applied

## Prerequisites
- Firebase project with AI Logic enabled
- google-services.json in app/ directory
- RECORD_AUDIO and INTERNET permissions
- Active internet connection on device

## How to Run
1. Add google-services.json to `experiments/voice/003-gemini-voice-loop/app/`
2. Open project in Android Studio Canary
3. Launch smartphone AVD and AI glasses AVD, pair them
4. Run with smartphone AVD as target
5. On glasses display, tap "Start" to begin voice conversation
6. Speak to glasses -> Gemini responds via audio
7. Tap "Stop" to end conversation

## Findings
(To be filled after test and review)

## Extracted Patterns
(To be filled after pass)
