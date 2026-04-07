package com.example.voicetouchpad

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import com.example.voicetouchpad.ui.VoiceTouchpadScreen
import com.example.voicetouchpad.ui.TtsStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesVoiceTouchpad"
    }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)
    private var ttsStatus by mutableStateOf(TtsStatus.Idle)
    private var currentCardIndex by mutableIntStateOf(0)

    private val cards = listOf(
        "Welcome to AI Glasses" to "Swipe forward to navigate. Voice confirms each action.",
        "Settings" to "Adjust display brightness and audio volume.",
        "About" to "Android XR AI Glasses R and D experiment 011.",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
                tts?.stop()
                tts?.shutdown()
                tts = null
            }
        })

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                speakCurrentCard()
            } else {
                Log.e(TAG, "TTS init failed with status: $status")
            }
        }

        initializeGlassesFeatures()
        setContent {
            GlimmerTheme {
                VoiceTouchpadScreen(
                    cards = cards,
                    currentIndex = currentCardIndex,
                    areVisualsOn = areVisualsOn,
                    isVisualUiSupported = isVisualUiSupported,
                    ttsStatus = ttsStatus,
                    onSwipeForward = { navigateForward() },
                    onSwipeBackward = { navigateBackward() },
                    onClick = { speakCurrentCard() },
                )
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

    private fun navigateForward() {
        if (currentCardIndex < cards.size - 1) {
            currentCardIndex++
            speakCurrentCard()
        }
    }

    private fun navigateBackward() {
        if (currentCardIndex > 0) {
            currentCardIndex--
            speakCurrentCard()
        }
    }

    private fun speakCurrentCard() {
        val (title, description) = cards[currentCardIndex]
        val text = "$title. $description"
        tts?.let { engine ->
            if (ttsReady) {
                ttsStatus = TtsStatus.Speaking
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        ttsStatus = TtsStatus.Speaking
                    }
                    override fun onDone(utteranceId: String?) {
                        ttsStatus = TtsStatus.Completed
                    }
                    override fun onError(utteranceId: String?) {
                        ttsStatus = TtsStatus.Error
                    }
                })
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "card_${currentCardIndex}")
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
