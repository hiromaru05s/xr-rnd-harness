package com.example.hostdevicecontext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.hostdevicecontext.ui.HostContextScreen
import com.example.hostdevicecontext.ui.ContextInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {
    companion object { private const val TAG = "GlassesHostContext" }
    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var contextInfo by mutableStateOf(ContextInfo())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseDisplayController() }
        })
        initializeGlassesFeatures()
        exploreContexts()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    HostContextScreen(info = contextInfo, onVibratePhone = { vibratePhone() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); releaseDisplayController(); initializeGlassesFeatures() }
    override fun onResume() { super.onResume(); if (displayController == null) initializeGlassesFeatures() }
    override fun onStop() { super.onStop(); if (isFinishing) releaseDisplayController() }

    private fun exploreContexts() {
        val isProjected = ProjectedContext.isProjectedDeviceContext(this)
        val deviceName = ProjectedContext.getProjectedDeviceName(this) ?: "null"
        try {
            val hostContext = ProjectedContext.createHostDeviceContext(this)
            val isHostProjected = ProjectedContext.isProjectedDeviceContext(hostContext)
            contextInfo = ContextInfo(
                isGlassesContext = isProjected, deviceName = deviceName,
                hostContextAvailable = true, isHostProjected = isHostProjected, errorMessage = "",
            )
        } catch (e: IllegalStateException) {
            contextInfo = ContextInfo(
                isGlassesContext = isProjected, deviceName = deviceName,
                hostContextAvailable = false, isHostProjected = false,
                errorMessage = e.message ?: "Unknown error",
            )
        }
    }

    private fun vibratePhone() {
        try {
            val hostContext = ProjectedContext.createHostDeviceContext(this)
            val vibrator = hostContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) { Log.e(TAG, "Vibration failed", e) }
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
            } catch (e: Exception) { Log.e(TAG, "Init failed", e) }
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { ctrl -> try { ctrl.close() } catch (e: Exception) { Log.w(TAG, "Error", e) } }
        displayController = null
    }
}
