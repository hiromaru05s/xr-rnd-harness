package com.example.camerageminiqa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object { private const val TAG = "GlassesMainActivity" }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var appState by mutableStateOf<AppState>(AppState.Initializing)
    private var cameraManager: CameraManager? = null
    private var geminiManager: GeminiVisionManager? = null
    private var projectedActivityCompat: ProjectedActivityCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseAllResources() }
        })
        initializeAll()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    VisualQAScreen(appState = appState, onCapture = { triggerCapture() }, onRetry = { retryFromError() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releaseDisplayController()
        initializeDisplayController()
    }

    override fun onResume() {
        super.onResume()
        if (displayController == null) { initializeDisplayController() }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) { releaseDisplayController() }
    }

    private fun initializeAll() {
        initializeDisplayController()
        initializeCamera()
        initializeGemini()
        startListeningForInputEvents()
    }

    private fun initializeDisplayController() {
        val act = this
        lifecycleScope.launch {
            try {
                val deviceController = ProjectedDeviceController.create(act)
                isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)
                val controller = ProjectedDisplayController.create(act)
                displayController = controller
                controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize display controller", e)
            }
        }
    }

    private fun initializeCamera() {
        val manager = CameraManager(this)
        cameraManager = manager
        manager.initialize { success ->
            if (success) { Log.d(TAG, "Camera initialized"); checkAllReady() }
            else { appState = AppState.Error("Camera initialization failed") }
        }
    }

    private fun initializeGemini() {
        val manager = GeminiVisionManager()
        geminiManager = manager
        lifecycleScope.launch {
            val success = manager.initialize()
            if (success) {
                manager.startConversation { transcript ->
                    runOnUiThread {
                        if (appState is AppState.Analyzing || appState is AppState.Conversing) {
                            appState = AppState.Conversing(lastTranscript = transcript)
                        }
                    }
                }
                checkAllReady()
            } else { appState = AppState.Error("Gemini session connection failed") }
        }
    }

    private fun startListeningForInputEvents() {
        val act = this
        lifecycleScope.launch {
            try {
                val compat = ProjectedActivityCompat.create(act)
                projectedActivityCompat = compat
                compat.projectedInputEvents.collect { event ->
                    runOnUiThread {
                        if (appState is AppState.Ready || appState is AppState.Conversing) { triggerCapture() }
                    }
                }
            } catch (e: Exception) { Log.w(TAG, "Input event listener failed", e) }
        }
    }

    private fun checkAllReady() {
        if (cameraManager != null && geminiManager != null && appState is AppState.Initializing) {
            appState = AppState.Ready
        }
    }

    private fun triggerCapture() {
        if (appState !is AppState.Ready && appState !is AppState.Conversing) return
        appState = AppState.Capturing
        cameraManager?.captureImage { result ->
            runOnUiThread {
                when (result) {
                    is CameraManager.CaptureResult.Success -> {
                        appState = AppState.Analyzing()
                        sendImageToGemini(result.base64Image, result.mimeType)
                    }
                    is CameraManager.CaptureResult.Failure -> {
                        appState = AppState.Error("Capture failed: " + result.message)
                    }
                }
            }
        }
    }

    private fun sendImageToGemini(base64Image: String, mimeType: String) {
        lifecycleScope.launch {
            try { geminiManager?.sendImage(base64Image, mimeType) }
            catch (e: Exception) { appState = AppState.Error("Image send failed") }
        }
    }

    private fun retryFromError() {
        releaseManagers()
        appState = AppState.Initializing
        initializeCamera()
        initializeGemini()
        startListeningForInputEvents()
    }

    private fun releaseDisplayController() {
        displayController?.let { try { it.close() } catch (_: Exception) {} }
        displayController = null
    }

    private fun releaseManagers() {
        cameraManager?.release(); cameraManager = null
        geminiManager?.release(); geminiManager = null
        projectedActivityCompat?.let { try { it.close() } catch (_: Exception) {} }
        projectedActivityCompat = null
    }

    private fun releaseAllResources() {
        releaseDisplayController()
        releaseManagers()
    }
}
