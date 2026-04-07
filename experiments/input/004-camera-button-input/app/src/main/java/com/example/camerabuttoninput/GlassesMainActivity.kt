package com.example.camerabuttoninput

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
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.camerabuttoninput.ui.CameraButtonScreen
import kotlinx.coroutines.launch

/**
 * グラス側アクティビティ: カメラボタンイベントの受信と表示。
 *
 * ProjectedActivityCompatのprojectedInputEventsフローを使って
 * カメラボタン(TOGGLE_APP_CAMERA)のイベントを検出する。
 */
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesMainActivity"
    }

    private var displayController: ProjectedDisplayController? = null
    private var projectedActivityCompat: ProjectedActivityCompat? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    // カメラボタン押下回数
    private var cameraButtonCount by mutableIntStateOf(0)

    // イベント受信状態
    private var isListening by mutableStateOf(false)
    private var lastEventDescription by mutableStateOf("イベント待機中")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseResources()
            }
        })

        initializeGlassesFeatures()
        startListeningForInputEvents()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    CameraButtonScreen(
                        cameraButtonCount = cameraButtonCount,
                        isListening = isListening,
                        lastEventDescription = lastEventDescription,
                        onReset = { cameraButtonCount = 0 },
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
        if (displayController == null) {
            initializeGlassesFeatures()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            releaseResources()
        }
    }

    /**
     * ProjectedActivityCompatを使ってカメラボタンのイベントを監視する。
     * projectedInputEventsフローはProjectedInputEventを発行し、
     * inputActionプロパティでアクション種別を判定する。
     */
    private fun startListeningForInputEvents() {
        lifecycleScope.launch {
            try {
                val compat = ProjectedActivityCompat.create(this@GlassesMainActivity)
                projectedActivityCompat = compat
                isListening = true
                lastEventDescription = "イベント監視中"

                compat.projectedInputEvents.collect { event ->
                    // ProjectedInputEventのinputActionで種別判定
                    val actionName = event.inputAction.toString()
                    cameraButtonCount++
                    lastEventDescription = "入力イベント検出 (#$cameraButtonCount): $actionName"
                    Log.d(TAG, "Input event received: action=$actionName, count=$cameraButtonCount")
                }
            } catch (e: Exception) {
                isListening = false
                lastEventDescription = "エラー: ${e.message}"
                Log.e(TAG, "Failed to listen for input events", e)
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
                Log.e(TAG, "Failed to initialize glasses features", e)
            }
        }
    }

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

    private fun releaseResources() {
        releaseDisplayController()
        projectedActivityCompat?.let { compat ->
            try {
                compat.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing ProjectedActivityCompat", e)
            }
        }
        projectedActivityCompat = null
        isListening = false
    }
}
