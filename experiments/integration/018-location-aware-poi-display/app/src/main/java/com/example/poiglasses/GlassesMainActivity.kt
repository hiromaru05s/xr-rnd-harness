package com.example.poiglasses

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import kotlinx.coroutines.launch
import kotlin.math.atan2

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object { private const val TAG = "GlassesMainActivity" }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var poiState by mutableStateOf<PoiState>(PoiState.Initializing)
    private var currentPoiIndex by mutableStateOf(0)
    private var sortedPois = listOf<PoiItem>()
    private var tts: TextToSpeech? = null
    private var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseAllResources() }
        })
        initializeDisplayController()
        initializeTts()
        initializeGeospatial()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    PoiScreen(poiState = poiState,
                        onSwipeForward = { navigateNext() },
                        onSwipeBackward = { navigatePrev() },
                        onClick = { speakCurrentPoi() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        releaseDisplayController(); initializeDisplayController()
    }

    override fun onResume() {
        super.onResume()
        if (displayController == null) { initializeDisplayController() }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) { releaseDisplayController() }
    }

    private fun initializeDisplayController() {
        val act = this
        lifecycleScope.launch {
            try {
                val dc = ProjectedDeviceController.create(act)
                isVisualUiSupported = dc.capabilities.contains(CAPABILITY_VISUAL_UI)
                val ctrl = ProjectedDisplayController.create(act)
                displayController = ctrl
                ctrl.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ctrl.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                }
            } catch (e: Exception) { Log.e(TAG, "Display init failed", e) }
        }
    }

    private fun initializeTts() {
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS init failed")
            }
        }
    }

    private fun initializeGeospatial() {
        val act = this
        lifecycleScope.launch {
            try {
                val result = Session.create(act)
                when (result) {
                    is SessionCreateSuccess -> {
                        val sess = result.session
                        session = sess
                        val config = Config(
                            geospatial = GeospatialMode.VPS_AND_GPS,
                            deviceTracking = DeviceTrackingMode.LAST_KNOWN)
                        sess.configure(config)
                        val geospatial = Geospatial.getInstance(sess)
                        val arDevice = ArDevice.getInstance(sess)
                        poiState = PoiState.WaitingForLocation
                        arDevice.state.collect { deviceState ->
                            val pose = deviceState.devicePose
                            if (pose != Pose.Identity) {
                                val heading = poseToHeading(pose)
                                val geoResult = geospatial.createGeospatialPoseFromPose(pose)
                                when (geoResult) {
                                    is CreateGeospatialPoseFromPoseSuccess -> {
                                        val gp = geoResult.pose
                                        updatePoisWithLocation(gp.latitude, gp.longitude, heading)
                                    }
                                    else -> { }
                                }
                            }
                        }
                    }
                    else -> {
                        poiState = PoiState.Error("Session creation failed")
                    }
                }
            } catch (e: Exception) { poiState = PoiState.Error("Geospatial init failed: " + (e.message ?: "")) }
        }
    }

    private fun poseToHeading(pose: Pose): Double {
        val q = pose.rotation
        val sinYaw = 2.0 * (q.w * q.z + q.x * q.y)
        val cosYaw = 1.0 - 2.0 * (q.y * q.y + q.z * q.z)
        return (Math.toDegrees(atan2(sinYaw.toDouble(), cosYaw.toDouble())) + 360) % 360
    }

    private fun updatePoisWithLocation(lat: Double, lng: Double, heading: Double) {
        val pois = PoiDataSource.getSamplePois().map { poi ->
            val dist = GeoUtils.distanceMeters(lat, lng, poi.latitude, poi.longitude)
            val bearing = GeoUtils.bearingDegrees(lat, lng, poi.latitude, poi.longitude)
            val dir = GeoUtils.relativeDirection(bearing, heading)
            poi.copy(distanceMeters = dist, bearingDegrees = bearing, relativeDirection = dir)
        }.sortedBy { it.distanceMeters }.take(3)
        sortedPois = pois
        if (pois.isNotEmpty()) {
            if (currentPoiIndex >= pois.size) currentPoiIndex = 0
            poiState = PoiState.Tracking(
                currentPoi = pois[currentPoiIndex],
                allPois = pois,
                currentIndex = currentPoiIndex,
                deviceHeading = heading,
                latitude = lat,
                longitude = lng)
        }
    }

    private fun navigateNext() {
        if (sortedPois.isEmpty()) return
        currentPoiIndex = (currentPoiIndex + 1).coerceAtMost(sortedPois.size - 1)
        updatePoiState()
        speakCurrentPoi()
    }

    private fun navigatePrev() {
        if (sortedPois.isEmpty()) return
        currentPoiIndex = (currentPoiIndex - 1).coerceAtLeast(0)
        updatePoiState()
        speakCurrentPoi()
    }

    private fun speakCurrentPoi() {
        val poi = sortedPois.getOrNull(currentPoiIndex) ?: return
        val dist = GeoUtils.formatDistance(poi.distanceMeters)
        val text = poi.name + ". " + dist + ". " + poi.relativeDirection
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "poi_" + currentPoiIndex)
    }

    private fun updatePoiState() {
        val poi = sortedPois.getOrNull(currentPoiIndex) ?: return
        val current = poiState
        if (current is PoiState.Tracking) {
            poiState = current.copy(currentPoi = poi, currentIndex = currentPoiIndex)
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { try { it.close() } catch (_: Exception) {} }
        displayController = null
    }

    private fun releaseAllResources() {
        releaseDisplayController()
        tts?.stop(); tts?.shutdown(); tts = null
        session = null
    }
}
