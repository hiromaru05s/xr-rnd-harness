# 003: Gemini Live Voice Conversation Loop - Findings

## Test Results
- All static checks (T1-T7) PASS
- Emulator: Skipped (requires Firebase config for runtime)

## Review Results
- Score: 9/10 (PASS)
- UX: 3/3, Code: 2/3, Reusability: 2/2, Documentation: 2/2
- Note: -1 for missing explicit LiveSession.close() in stopConversation

## Key Findings

### Gemini Live API Integration
1. **Firebase AI Logic**: Use Firebase.ai(backend = GenerativeBackend.googleAI())
   to access Gemini Live API. Requires google-services.json and firebase-bom.
2. **Model**: gemini-2.5-flash-native-audio-preview-12-2025 for native audio.
3. **Native audio**: ResponseModality.AUDIO lets Gemini generate speech directly,
   eliminating the need for a separate TTS pipeline for responses.
4. **Session lifecycle**: model.connect() creates a session, startAudioConversation()
   begins the voice loop. Session should be explicitly closed when done.
5. **Transcript handler**: transcriptHandler callback provides real-time text of
   what Gemini is hearing/saying. Useful for glasses UI display.
6. **Interruption support**: enableInterruptions=true allows natural turn-taking.

### Voice-First UI Design for Glasses
1. **Minimal states**: 6 states (IDLE/CONNECTING/LISTENING/THINKING/SPEAKING/ERROR)
   are sufficient for a glance-able voice UI.
2. **TitleChip as state indicator**: Icon + state label in TitleChip provides
   at-a-glance status. Mic icon for listening, Info for error.
3. **Card for transcript**: Single Card showing either prompt (IDLE) or last
   transcript (active conversation).
4. **Action button**: Start (Mic icon) / Stop (MicOff icon) in Card action slot.
5. **GlimmerTheme.colors.negative**: Use for Stop button to indicate destructive action.

### TTS as Fallback
1. **Error notification**: When Gemini connection fails, TTS speaks error message.
2. **Built-in API**: TextToSpeech requires no additional dependencies.
3. **Lifecycle**: Initialize in onCreate, shutdown in onDestroy via LifecycleObserver.

### Known Limitations
1. **No explicit session close**: Current implementation resets state but does not
   call session.close(). Should store session reference and close on stop.
2. **No audio permission request UI**: RECORD_AUDIO permission is assumed granted.
   Production code should use ProjectedPermissionsRequestParams.
3. **No offline fallback**: If internet is unavailable, conversation cannot start.
   Consider caching last responses or providing offline TTS guidance.

## Implications for Next Experiments
- Function calling with Gemini Live could enable voice-controlled app features
- Camera + voice combination: "What am I looking at?" use case
- Multi-turn conversation history could be displayed in VerticalList
- Voice + touchpad hybrid: voice for commands, touchpad for navigation
