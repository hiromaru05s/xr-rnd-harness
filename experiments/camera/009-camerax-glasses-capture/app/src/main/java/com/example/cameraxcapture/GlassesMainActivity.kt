package com.example.cameraxcapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import com.example.cameraxcapture.ui.CameraXScreen
import com.example.cameraxcapture.ui.CaptureState
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesCameraActivity"
    }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var captureState by mutableStateOf<CaptureState>(CaptureState.Initializing)
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher: ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            val cameraGranted = results[Manifest.permission.CAMERA] == true
            if (cameraGranted) {
                captureState = CaptureState.PermissionGranted
                initializeCamera()
            } else {
                captureState = CaptureState.PermissionDenied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseDisplayController() }
        })
        initializeGlassesFeatures()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    CameraXScreen(
                        captureState = captureState,
                        onRetryPermission = { requestCameraPermission() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releaseDisplayController()
        initializeGlassesFeatures()
    }

    override fun onResume() {
        super.onResume()
        if (displayController == null) initializeGlassesFeatures()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) releaseDisplayController()
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            try {
                val dc = ProjectedDeviceController.create(this@GlassesMainActivity)
                isVisualUiSupported = dc.capabilities.contains(CAPABILITY_VISUAL_UI)
                val ctrl = ProjectedDisplayController.create(this@GlassesMainActivity)
                displayController = ctrl
                ctrl.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ctrl.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                }
                if (hasCameraPermission()) {
                    captureState = CaptureState.PermissionGranted
                    initializeCamera()
                } else {
                    requestCameraPermission()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
                captureState = CaptureState.Error("Initialization failed: ${e.message}")
            }
        }
    }

    private fun initializeCamera() {
        captureState = CaptureState.BindingCamera
        try {
            val projectedContext = ProjectedContext.createProjectedDeviceContext(this)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(projectedContext)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        captureState = CaptureState.Error("No back camera found on glasses")
                        return@addListener
                    }
                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER
                            )
                        ).build()
                    val capture = ImageCapture.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this@GlassesMainActivity, cameraSelector, capture)
                    imageCapture = capture
                    captureState = CaptureState.Ready(resolution = "640x480", cameraSelector = "DEFAULT_BACK_CAMERA")
                    Log.i(TAG, "CameraX bound to lifecycle via projected context")
                } catch (e: Exception) {
                    Log.e(TAG, "CameraX bind failed", e)
                    captureState = CaptureState.Error("Camera bind failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "ProjectedDeviceContext creation failed", e)
            captureState = CaptureState.Error("Projected context failed: ${e.message}")
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        captureState = CaptureState.RequestingPermission
        val params = ProjectedPermissionsRequestParams(
            permissions = listOf(Manifest.permission.CAMERA),
            rationale = "Camera access is required to capture images from AI glasses."
        )
        requestPermissionLauncher.launch(listOf(params))
    }

    private fun releaseDisplayController() {
        displayController?.let { ctrl ->
            try { ctrl.close() } catch (e: Exception) {
                Log.w(TAG, "Error closing DisplayController", e)
            }
        }
        displayController = null
    }
}
