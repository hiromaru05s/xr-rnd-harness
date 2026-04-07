package com.example.geospatiallocation

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
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import com.example.geospatiallocation.ui.GeospatialScreen
import com.example.geospatiallocation.ui.GeoState
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesGeoActivity"
    }

    private var displayController: ProjectedDisplayController? = null
    private var session: Session? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var geoState by mutableStateOf<GeoState>(GeoState.Initializing)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
                session = null
            }
        })
        initializeGlassesFeatures()
        initializeGeospatial()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    GeospatialScreen(geoState = geoState)
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

    private fun initializeGeospatial() {
        lifecycleScope.launch {
            geoState = GeoState.CreatingSession
            try {
                when (val result = Session.create(this@GlassesMainActivity)) {
                    is SessionCreateSuccess -> {
                        val xrSession = result.session
                        session = xrSession
                        val config = Config(
                            geospatial = GeospatialMode.VPS_AND_GPS,
                            deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                        )
                        xrSession.configure(config)
                        geoState = GeoState.SessionReady

                        val geospatial = Geospatial.getInstance(xrSession)
                        val arDevice = ArDevice.getInstance(xrSession)

                        arDevice.state.collect { deviceState ->
                            val pose = deviceState.devicePose
                            val isTracking = pose != Pose.Identity
                            if (isTracking) {
                                when (val geoResult = geospatial.createGeospatialPoseFromPose(pose)) {
                                    is CreateGeospatialPoseFromPoseSuccess -> {
                                        val geoPose = geoResult.pose
                                        geoState = GeoState.Tracking(
                                            latitude = geoPose.latitude,
                                            longitude = geoPose.longitude,
                                            altitude = geoPose.altitude,
                                            trackingState = "TRACKING",
                                        )
                                    }
                                    else -> {
                                        geoState = GeoState.NotTracking(
                                            reason = "Geo pose: " + geoResult::class.simpleName
                                        )
                                    }
                                }
                            } else {
                                geoState = GeoState.NotTracking(reason = "Waiting for device tracking")
                            }
                        }
                    }
                    else -> {
                        geoState = GeoState.Error("Session: " + result::class.simpleName)
                    }
                }
            } catch (e: Exception) {
                geoState = GeoState.Error("Error: ${e.message}")
                Log.e(TAG, "Geospatial init failed", e)
            }
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
