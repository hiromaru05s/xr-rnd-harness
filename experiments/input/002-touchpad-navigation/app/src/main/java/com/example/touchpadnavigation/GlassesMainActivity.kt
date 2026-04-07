package com.example.touchpadnavigation

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
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.touchpadnavigation.ui.TouchpadNavigationScreen
import kotlinx.coroutines.launch

/**
 * AIグラス用アクティビティ。
 * 堅牢ライフサイクルパターン（001で確立）を適用。
 * singleTop + onNewIntent/onResume再初期化で再表示問題を防止。
 */
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
            }
        })

        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    TouchpadNavigationScreen()
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
        if (displayController == null) {
            initializeGlassesFeatures()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            releaseDisplayController()
        }
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            try {
                val deviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
                isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)

                val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
                displayController = controller
                controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                controller.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize glasses features", e)
            }
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { controller ->
            try { controller.close() } catch (e: Exception) {
                Log.w(TAG, "Error closing DisplayController", e)
            }
        }
        displayController = null
    }

    companion object {
        private const val TAG = "GlassesMainActivity"
    }
}
