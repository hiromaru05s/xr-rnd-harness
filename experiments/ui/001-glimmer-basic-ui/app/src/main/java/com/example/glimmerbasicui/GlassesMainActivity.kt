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
 * AIグラス用アクティビティ。
 *
 * ProjectedContext 経由で起動され、グラスの透過ディスプレイ上に描画される。
 * ProjectedDisplayController でスクリーンオン維持、
 * ProjectedDeviceController でディスプレイ能力を確認する。
 *
 * [FB対応] singleTopモードで起動し、onNewIntent/onResumeでも
 * DisplayControllerの再初期化を行うことで、別画面から戻った際の
 * UI再表示問題を解決。
 */
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // ライフサイクルオブザーバーでDisplayControllerをクリーンアップ
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                Log.d(TAG, "onDestroy: closing DisplayController")
                releaseDisplayController()
            }
        })

        // グラスハードウェア初期化
        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                // isVisualUiSupportedとareVisualsOnの両方がtrueの場合のみUIを描画
                if (isVisualUiSupported && areVisualsOn) {
                    BasicUiScreen()
                }
            }
        }
    }

    /**
     * [FB対応] singleTopモードで既存インスタンスが再利用される場合、
     * onNewIntentが呼ばれる。ここでDisplayControllerを再初期化する。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: re-initializing glasses features")
        releaseDisplayController()
        initializeGlassesFeatures()
    }

    /**
     * [FB対応] onResumeでDisplayControllerがnullなら再初期化。
     * バックグラウンドから復帰した際にUIが表示されなくなる問題を防ぐ。
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: displayController=${displayController != null}")
        if (displayController == null) {
            initializeGlassesFeatures()
        }
    }

    /**
     * onStopではDisplayControllerを解放しない。
     * 再びonResumeで復帰する可能性があるため。
     * ただし、isFinishing==trueの場合は即座に解放する。
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
                // デバイス能力チェック（ディスプレイ有無）
                val deviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
                isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)
                Log.d(TAG, "Visual UI supported: $isVisualUiSupported")

                // ディスプレイコントローラー初期化
                val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
                displayController = controller

                // スクリーンオン維持（グラスディスプレイのスヌーズ防止）
                controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // プレゼンテーションモード監視
                controller.addPresentationModeChangedListener { flags ->
                    areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                    Log.d(TAG, "Visuals on: $areVisualsOn")
                }

                Log.d(TAG, "Glasses features initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize glasses features", e)
                // 初期化失敗時でもクラッシュしない。UIは表示されないがアプリは生存する。
            }
        }
    }

    /**
     * DisplayControllerを安全に解放する。
     * 複数回呼ばれても問題ないようにnullチェック付き。
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
