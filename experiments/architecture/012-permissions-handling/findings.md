# 012: Permissions Handling - Findings

## Review Score: 10/10 (PASS)

## Key Discoveries
1. **ProjectedPermissionsResultContract** from `projected.permissions` package handles glasses permission UI.
2. **Multiple permissions** can be requested in single ProjectedPermissionsRequestParams.
3. **PermissionItemState enum** (4 states) provides cleaner tracking than boolean flags.
4. **checkExistingPermissions() in onResume** keeps state fresh after app backgrounding.
5. **TTS fallback** on permission denial ensures accessibility even without visual permission UI.

## API Insights
- ProjectedPermissionsRequestParams takes `permissions: List<String>` and `rationale: String?`
- Result map keys are permission strings (e.g., Manifest.permission.CAMERA)
- Permission classes are in `androidx.xr.projected.permissions` (sub-package)
- Re-request after denial is supported (no need for shouldShowRequestPermissionRationale)

## Patterns Extracted
- Glasses permission request flow pattern
- Progressive permission UI with state tracking
