package com.example.glimmertypography

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.glimmertypography.ui.TypographyScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesTypography"
    }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var currentPage by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseDisplayController() }
        })
        initializeGlassesFeatures()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    TypographyScreen(
                        currentPage = currentPage,
                        onSwipeForward = { if (currentPage < 2) currentPage++ },
                        onSwipeBackward = { if (currentPage > 0) currentPage-- },
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
