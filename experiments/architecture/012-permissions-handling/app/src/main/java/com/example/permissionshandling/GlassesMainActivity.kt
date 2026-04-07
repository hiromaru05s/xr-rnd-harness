package com.example.permissionshandling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.permissionshandling.ui.PermissionsScreen
import com.example.permissionshandling.ui.PermissionItemState
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesPermissions"
    }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var cameraPermState by mutableStateOf(PermissionItemState.NOT_REQUESTED)
    private var audioPermState by mutableStateOf(PermissionItemState.NOT_REQUESTED)
    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)

    private val requestPermissionLauncher: ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            cameraPermState = if (results[Manifest.permission.CAMERA] == true) {
                PermissionItemState.GRANTED
            } else {
                PermissionItemState.DENIED
            }
            audioPermState = if (results[Manifest.permission.RECORD_AUDIO] == true) {
                PermissionItemState.GRANTED
            } else {
                PermissionItemState.DENIED
            }
            if (cameraPermState == PermissionItemState.DENIED || audioPermState == PermissionItemState.DENIED) {
                speakFallback("Some permissions were denied. You can retry from the UI.")
            } else {
                speakFallback("All permissions granted.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
                tts?.stop()
                tts?.shutdown()
                tts = null
            }
        })

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }

        checkExistingPermissions()
        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    PermissionsScreen(
                        cameraState = cameraPermState,
                        audioState = audioPermState,
                        onRequestPermissions = { requestAllPermissions() },
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
        checkExistingPermissions()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) releaseDisplayController()
    }

    private fun checkExistingPermissions() {
        cameraPermState = if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            PermissionItemState.GRANTED
        } else {
            if (cameraPermState == PermissionItemState.DENIED) PermissionItemState.DENIED else PermissionItemState.NOT_REQUESTED
        }
        audioPermState = if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            PermissionItemState.GRANTED
        } else {
            if (audioPermState == PermissionItemState.DENIED) PermissionItemState.DENIED else PermissionItemState.NOT_REQUESTED
        }
    }

    private fun requestAllPermissions() {
        cameraPermState = PermissionItemState.REQUESTING
        audioPermState = PermissionItemState.REQUESTING
        val params = ProjectedPermissionsRequestParams(
            permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            rationale = "Camera and microphone access are required for AI glasses features."
        )
        requestPermissionLauncher.launch(listOf(params))
    }

    private fun speakFallback(text: String) {
        if (ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "perm_feedback")
        }
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
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
            }
        }
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
