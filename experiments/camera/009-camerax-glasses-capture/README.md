# 009: CameraX Glasses Camera Capture

## Hypothesis
ProjectedContext.createProjectedDeviceContext() can acquire CameraX provider to access glasses outward-facing camera for image capture.

## Technologies
- Skills: glasses-hardware, projected-api, glimmer-api
- Libraries: CameraX (camera-camera2, camera-lifecycle), Projected API, Glimmer

## Implementation
- ProjectedContext.createProjectedDeviceContext() for glasses context
- ProcessCameraProvider.getInstance(projectedContext) for camera provider
- ImageCapture.Builder() with ResolutionSelector (640x480, FALLBACK_RULE_CLOSEST_LOWER)
- CaptureState sealed class state machine (7 states: Initializing, RequestingPermission, PermissionGranted, PermissionDenied, BindingCamera, Ready, Error)
- ProjectedPermissionsResultContract for camera permission request
- Glimmer UI reflecting capture state (TitleChip + Card)

## How to Run
1. Open in Android Studio Canary
2. Start Smartphone AVD and AI Glasses AVD
3. Target Smartphone AVD and run

## Findings
(After test/review)

## Extracted Patterns
(After PASS, reference patterns/ links)
