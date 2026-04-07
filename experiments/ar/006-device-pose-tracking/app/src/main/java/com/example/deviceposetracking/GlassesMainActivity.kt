package com.example.deviceposetracking

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
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import com.example.deviceposetracking.ui.PoseTrackingScreen
import kotlinx.coroutines.launch

/**
 * ARCore Session経由でデバイスポーズを取得し、UIに表示するグラスアクティビティ。
 */
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesMainActivity"
    }

    private var displayController: ProjectedDisplayController? = null
    private var session: Session? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    // ポーズ情報
    private var currentPose by mutableStateOf<Pose?>(null)
    private var trackingStatus by mutableStateOf("初期化中")
    private var sessionStatus by mutableStateOf("セッション未作成")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseResources()
            }
        })

        initializeGlassesFeatures()
        initializeArSession()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    PoseTrackingScreen(
                        currentPose = currentPose,
                        trackingState = trackingStatus,
                        sessionStatus = sessionStatus,
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
        if (isFinishing) releaseResources()
    }

    /**
     * XR Runtime Sessionを作成し、デバイストラッキングを有効化して
     * ArDeviceのポーズを継続的に監視する。
     */
    private fun initializeArSession() {
        lifecycleScope.launch {
            try {
                val createResult = Session.create(this@GlassesMainActivity)
                when (createResult) {
                    is SessionCreateSuccess -> {
                        val xrSession = createResult.session
                        session = xrSession
                        sessionStatus = "セッション作成成功"

                        // デバイストラッキング有効化
                        val config = Config(
                            deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                        )
                        xrSession.configure(config)
                        sessionStatus = "トラッキング設定完了"

                        // ArDeviceからポーズを継続的に収集
                        val arDevice = ArDevice.getInstance(xrSession)
                        arDevice.state.collect { state ->
                            currentPose = state.devicePose
                            // 状態をポーズ値で推定
                            trackingStatus = if (state.devicePose != Pose.Identity) {
                                "トラッキング中"
                            } else {
                                "ポーズ待機中"
                            }
                        }
                    }
                    else -> {
                        sessionStatus = "セッション作成失敗: ${createResult::class.simpleName}"
                        Log.e(TAG, "Session creation failed: $createResult")
                    }
                }
            } catch (e: Exception) {
                sessionStatus = "エラー: ${e.message}"
                Log.e(TAG, "AR session initialization failed", e)
            }
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
                Log.e(TAG, "Failed to initialize", e)
            }
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { try { it.close() } catch (e: Exception) { Log.w(TAG, "Error", e) } }
        displayController = null
    }

    private fun releaseResources() {
        releaseDisplayController()
        session = null
    }
}
