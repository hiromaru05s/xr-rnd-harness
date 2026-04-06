package com.example.glimmerbasicui

import android.os.Bundle
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
 */
@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ライフサイクルオブザーバーで DisplayController をクリーンアップ
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                displayController?.close()
                displayController = null
            }
        })

        // グラスハードウェア初期化
        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    BasicUiScreen()
                }
            }
        }
    }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            // デバイス能力チェック（ディスプレイ有無）
            val deviceController = ProjectedDeviceController.create(this@GlassesMainActivity)
            isVisualUiSupported = deviceController.capabilities.contains(CAPABILITY_VISUAL_UI)

            // ディスプレイコントローラー初期化
            val controller = ProjectedDisplayController.create(this@GlassesMainActivity)
            displayController = controller

            // スクリーンオン維持（グラスディスプレイのスヌーズ防止）
            controller.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // プレゼンテーションモード監視
            controller.addPresentationModeChangedListener { flags ->
                areVisualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
            }
        }
    }
}
