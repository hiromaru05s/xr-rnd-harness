# 011: Voice+Touchpad Integrated Navigation

## Hypothesis
TTS voice feedback and touchpad gesture navigation can be combined so that every navigation action gets voice confirmation, creating an accessible UI.

## Technologies
- Skills: glimmer-api, glasses-hardware, projected-api
- Libraries: TextToSpeech (Android built-in), Projected API, Glimmer (onIndirectPointerGesture)

## Implementation
- VerticalStack-style card navigation with touchpad swipe (onSwipeForward/onSwipeBackward)
- TextToSpeech with UtteranceProgressListener for voice feedback
- Auto-read on card switch (speakCurrentCard called on every navigation)
- PresentationMode-aware: Visual+Voice mode vs Voice-only mode
- TtsStatus enum (Idle/Speaking/Completed/Error)
- focusable() + onIndirectPointerGesture for gesture detection

## How to Run
1. Open in Android Studio Canary
2. Start Smartphone AVD and AI Glasses AVD
3. Target Smartphone AVD and run

## Findings
(After test/review)

## Extracted Patterns
(After PASS, reference patterns/ links)
