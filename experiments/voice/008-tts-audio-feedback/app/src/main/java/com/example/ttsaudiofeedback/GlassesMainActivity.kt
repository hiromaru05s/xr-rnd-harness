package com.example.ttsaudiofeedback

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
import com.example.ttsaudiofeedback.ui.TtsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {
    companion object { private const val TAG = "GlassesMainActivity" }

    private var displayController: ProjectedDisplayController? = null
    private var ttsManager: TtsManager? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var ttsStateText by mutableStateOf("初期化中")
    private var feedbackMode by mutableStateOf("視覚+音声")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseResources() }
        })

        // TtsManager初期化
        ttsManager = TtsManager(this) { state ->
            runOnUiThread {
                ttsStateText = when (state) {
                    is TtsManager.TtsState.Initializing -> "初期化中..."
                    is TtsManager.TtsState.Ready -> "準備完了"
                    is TtsManager.TtsState.Speaking -> "発話中: ${state.utteranceId}"
                    is TtsManager.TtsState.Completed -> "完了: ${state.utteranceId}"
                    is TtsManager.TtsState.Error -> "エラー: ${state.message}"
                }
            }
        }

        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    TtsScreen(
                        ttsState = ttsStateText,
                        feedbackMode = feedbackMode,
                        onSpeakWelcome = { ttsManager?.speakFlush("AIグラスへようこそ") },
                        onSpeakStatus = { ttsManager?.speakFlush("現在のモードは${feedbackMode}です") },
                        onStop = { ttsManager?.stop() },
                    )
                } else if (!areVisualsOn) {
                    // ディスプレイOFF時: TTSのみで情報伝達
                    ttsManager?.speakFlush("ディスプレイがオフです。音声ガイダンスモードに切り替えます。")
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); releaseDisplayController(); initializeGlassesFeatures() }
    override fun onResume() { super.onResume(); if (displayController == null) initializeGlassesFeatures() }
    override fun onStop() { super.onStop(); if (isFinishing) releaseResources() }

    private fun initializeGlassesFeatures() {
        lifecycleScope.launch {
            try {
                val dc = ProjectedDeviceController.create(this@GlassesMainActivity)
                isVisualUiSupported = dc.capabilities.contains(CAPABILITY_VISUAL_UI)
                val ctrl = ProjectedDisplayController.create(this@GlassesMainActivity)
                displayController = ctrl
                ctrl.addLayoutParamsFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                ctrl.addPresentationModeChangedListener { flags ->
                    val visualsOn = flags.hasPresentationMode(PresentationMode.VISUALS_ON)
                    areVisualsOn = visualsOn
                    feedbackMode = if (visualsOn) "視覚+音声" else "音声のみ"
                }
            } catch (e: Exception) { Log.e(TAG, "Init failed", e) }
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { try { it.close() } catch (e: Exception) { Log.w(TAG, "Error", e) } }
        displayController = null
    }

    private fun releaseResources() {
        releaseDisplayController()
        ttsManager?.shutdown()
        ttsManager = null
    }
}
