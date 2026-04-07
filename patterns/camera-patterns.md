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

## CameraX + Gemini Multimodal Visual Q&A

**When to use**: When capturing an image from glasses camera and sending it to Gemini Live for AI analysis and voice Q&A
**Prerequisites**: `implementation("androidx.camera:camera-camera2:1.4.0")`, `implementation("androidx.camera:camera-lifecycle:1.4.0")`, `implementation("com.google.firebase:firebase-ai")`, `implementation("androidx.xr.projected:projected:1.0.0-alpha05")`

```kotlin
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.xr.projected.ProjectedContext
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import java.io.ByteArrayOutputStream

// 1. Initialize camera with ProjectedDeviceContext
val projectedContext = ProjectedContext.createProjectedDeviceContext(activity)
val cameraProvider = ProcessCameraProvider.getInstance(projectedContext).get()
val imageCapture = ImageCapture.Builder()
    .setResolutionSelector(ResolutionSelector.Builder()
        .setResolutionStrategy(ResolutionStrategy(
            Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
        .build())
    .build()
cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, imageCapture)

// 2. Capture image and convert to Base64
imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        imageProxy.close()
        // base64 is ready for Gemini
    }
})

// 3. Initialize Gemini Live session
val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
    modelName = "gemini-2.5-flash-native-audio-preview-12-2025",
    generationConfig = liveGenerationConfig {
        responseModality = ResponseModality.AUDIO
        speechConfig = SpeechConfig(voice = Voice("FENRIR"))
    },
    systemInstruction = content { text("You are a visual assistant for AI glasses.") },
)
val session = model.connect()
session.startAudioConversation(transcriptHandler = { transcript -> /* update UI */ })

// 4. Send captured image to Gemini for analysis
val imageData = Base64.decode(base64, Base64.NO_WRAP)
session.send(content {
    inlineData("image/jpeg", imageData)
    text("What do you see in this image?")
})
// Gemini will respond via audio automatically
```

**Gotchas**:
- Must use `ProjectedContext.createProjectedDeviceContext()` for glasses camera access
- ImageProxy.planes[0].buffer may be JPEG or YUV depending on device - handle both
- Base64.NO_WRAP is important for inline data (no line breaks)
- Gemini processes image+text as multimodal input and responds via audio
- Camera button events from ProjectedActivityCompat make natural capture triggers
- 640x480 resolution recommended for AI analysis (battery/thermal constraints)

**Source**: experiments/integration/016-camera-gemini-visual-qa

---
