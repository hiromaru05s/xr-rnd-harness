# 017: Notification Voice Bridge

## Hypothesis
Received notifications can be automatically read aloud via TTS, with touchpad swipe for next/re-read/dismiss operations.

## Technologies Used
- Skills: glimmer-api, projected-api, glasses-hardware, glasses-arch
- Libraries: Glimmer, Projected API, Android TTS, NotificationListenerService

## Implementation
- NotificationDataSource simulates incoming notifications with realistic data
- NotificationQueueManager maintains a 3-item rolling queue (FOV compliant)
- TTS auto-reads each notification on arrival and on touchpad swipe
- Touchpad: swipe forward = next, swipe backward = re-read, click = dismiss
- PresentationMode-aware: Visual+Voice or Voice Only mode

## Execution
1. Open in Android Studio Canary
2. Launch on phone AVD with glasses AVD
3. Notifications are auto-generated for demo

## Findings
(To be added after test/review)

## Extracted Patterns
(To be added after PASS)
