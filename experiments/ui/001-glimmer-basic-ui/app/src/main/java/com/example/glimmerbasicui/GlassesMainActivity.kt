package com.example.glimmerbasicui

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
import com.example.glimmerbasicui.ui.BasicUiScreen
import kotlinx.coroutines.launch

/**
 * Glasses-side activity for AI glasses display.
 *
 * Launched via ProjectedContext and renders on the see-through display.
 * Uses ProjectedDisplayController for screen-on persistence and
 * ProjectedDeviceController for display capability checks.
 *
 * [FB fix #2] singleTop launch mode + onNewIntent/onResume reinit
 * to resolve UI re-display issue after navigating away.
 */
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // Lifecycle observer for DisplayController cleanup
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                Log.d(TAG, "onDestroy: closing DisplayController")
                releaseDisplayController()
            }
        })

        // Initialize glasses hardware
        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                // Only render UI when both visual capability and visuals-on are true
                if (isVisualUiSupported && areVisualsOn) {
                    BasicUiScreen()
                }
            }
        }
    }

    /**
     * [FB fix #2] When singleTop reuses existing instance,
     * onNewIntent is called. Reinitialize DisplayController here.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: re-initializing glasses features")
        releaseDisplayController()
        initializeGlassesFeatures()
    }

    /**
     * [FB fix #2] Reinitialize DisplayController if null on resume.
     * Prevents UI from disappearing after returning from background.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: displayController=${displayController != null}")
        if (displayController == null) {
            initializeGlassesFeatures()
        }
    }

    /**
     * Do not release DisplayController in onStop — activity may resume.
     * Only release immediately if isFinishing is true.
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: isFinishing=$isFinishing")
        if (isFinishing) {
            releaseDisplayController()
        }
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            try {
                // Check device capability (display available)
                val deviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
                isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)
                Log.d(TAG, "Visual UI supported: $isVisualUiSupported")

                // Initialize display controller
                val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
                displayController = controller

                // Keep screen on (prevent glasses display snooze)
                controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // Monitor presentation mode
                controller.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                    Log.d(TAG, "Visuals on: $areVisualsOn")
                }

                Log.d(TAG, "Glasses features initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize glasses features", e)
                // Do not crash on init failure. UI won't show but app survives.
            }
        }
    }

    /**
     * Safely release DisplayController.
     * Null-check ensures safe repeated calls.
     */
    private fun releaseDisplayController() {
        displayController?.let { controller ->
            try {
                controller.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing DisplayController", e)
            }
        }
        displayController = null
    }

    companion object {
        private const val TAG = "GlassesMainActivity"
    }
}
