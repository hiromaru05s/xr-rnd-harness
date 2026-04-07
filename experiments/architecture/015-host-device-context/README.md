# 015: Host Device Context for Phone Hardware Access

## Hypothesis
ProjectedContext.createHostDeviceContext() enables accessing phone hardware (vibration) from glasses Activity.

## Technologies
- Skills: projected-api, glimmer-api
- Libraries: Projected API (ProjectedContext), Glimmer, Android Vibrator

## Implementation
- createHostDeviceContext(activity) for phone context from glasses
- isProjectedDeviceContext() for context type detection
- getProjectedDeviceName() for device identification
- Vibrator access via host context
- ContextInfo data class for clean state

## How to Run
1. Open in Android Studio Canary
2. Start Smartphone AVD and AI Glasses AVD
3. Target Smartphone AVD and run

## Findings
(After test/review)
