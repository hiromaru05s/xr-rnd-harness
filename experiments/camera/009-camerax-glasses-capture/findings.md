# 009: CameraX Glasses Camera Capture - Findings

## Review Score: 10/10 (PASS)

## Key Discoveries
1. **ProjectedDeviceContext is essential** for accessing glasses camera. Standard `this` context only accesses phone camera.
2. **ResolutionSelector with FALLBACK_RULE_CLOSEST_LOWER** ensures graceful degradation when exact resolution unavailable.
3. **CaptureState sealed class** provides clean state machine for camera lifecycle (7 states).
4. **ProjectedPermissionsResultContract** (from `permissions` sub-package) handles glasses-specific permission dialogs.
5. **CameraSelector.DEFAULT_BACK_CAMERA** maps to glasses outward-facing camera in projected context.

## API Insights
- `ProcessCameraProvider.getInstance()` accepts projected context (not just Activity context)
- `ResolutionSelector` and `ResolutionStrategy` are in `camera.core.resolutionselector` package
- `ProjectedPermissionsRequestParams/ResultContract` are in `projected.permissions` package
- Camera binding to lifecycle works with `bindToLifecycle(activity, selector, useCase)`

## Patterns Extracted
- CameraX via projected context initialization pattern
- CaptureState sealed class state machine
