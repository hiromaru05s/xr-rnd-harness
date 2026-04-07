# cameraパターン集

> このファイルはAIがコンテキストとして読み込み、vibe codingの参照にする。
> コードスニペットはコピペで動くレベルの完全性を維持すること。

---

（まだパターンなし。実験がPASSするごとにOrchestratorが追記する）

## CameraX via ProjectedDeviceContext

**When to use**: When accessing the glasses outward-facing camera via CameraX
**Prerequisites**: `implementation("androidx.camera:camera-camera2:1.4.0")`, `implementation("androidx.camera:camera-lifecycle:1.4.0")`, `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`

```kotlin
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi

@OptIn(ExperimentalProjectedApi::class)
private fun initializeCamera(activity: ComponentActivity) {
    val projectedContext = ProjectedContext.createProjectedDeviceContext(activity)
    val cameraProviderFuture = ProcessCameraProvider.getInstance(projectedContext)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        if (!cameraProvider.hasCamera(cameraSelector)) return@addListener

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                )
            ).build()

        val imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(activity, cameraSelector, imageCapture)
    }, ContextCompat.getMainExecutor(activity))
}
```

**Gotchas**:
- Must use `ProjectedContext.createProjectedDeviceContext()` NOT `this` for glasses camera
- `ResolutionSelector` and `ResolutionStrategy` are in `camera.core.resolutionselector` package
- 640x480 recommended for computer vision, 1280x720 for video
- `DEFAULT_BACK_CAMERA` maps to glasses outward-facing camera in projected context
- Battery/thermal constraints: minimize resolution and frame rate

**Source**: experiments/camera/009-camerax-glasses-capture

---
