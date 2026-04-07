package com.example.notifvoice

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object { private const val TAG = "GlassesMainActivity" }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var bridgeState by mutableStateOf<BridgeState>(BridgeState.Initializing)
    private var ttsStatus by mutableStateOf(TtsStatus.Idle)
    private val queueManager = NotificationQueueManager()
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private var demoRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseAllResources() }
        })
        initializeDisplayController()
        initializeTts()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    NotificationBridgeScreen(
                        bridgeState = bridgeState, ttsStatus = ttsStatus,
                        onSwipeForward = { navigateForward() },
                        onSwipeBackward = { navigateBackward() },
                        onClick = { dismissCurrent() })
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
            if (status == TextToSpeech.SUCCESS) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(uid: String) { runOnUiThread { ttsStatus = TtsStatus.Speaking } }
                    override fun onDone(uid: String) { runOnUiThread { ttsStatus = TtsStatus.Completed } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(uid: String) { runOnUiThread { ttsStatus = TtsStatus.ErrorOccurred } }
                })
                bridgeState = BridgeState.WaitingForNotifications
                startDemoNotifications()
            } else { bridgeState = BridgeState.Error("TTS init failed") }
        }
    }

    private fun startDemoNotifications() {
        demoRunnable = object : Runnable {
            override fun run() {
                onNotificationReceived(NotificationDataSource.generateNext())
                handler.postDelayed(this, 8000)
            }
        }
        handler.postDelayed(demoRunnable!!, 2000)
    }

    private fun onNotificationReceived(item: NotificationItem) {
        queueManager.addNotification(item)
        updateBridgeState()
        speakNotification(item)
    }

    private fun speakNotification(item: NotificationItem) {
        val text = item.appName + ". " + item.title + ". " + item.text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notif_" + item.id)
    }

    private fun navigateForward() {
        val n = queueManager.moveToNext()
        updateBridgeState()
        if (n != null) speakNotification(n)
    }

    private fun navigateBackward() {
        val p = queueManager.moveToPrevious()
        updateBridgeState()
        if (p != null) speakNotification(p)
    }

    private fun dismissCurrent() {
        val n = queueManager.dismissCurrent()
        updateBridgeState()
        if (n != null) speakNotification(n)
        else { tts?.stop(); ttsStatus = TtsStatus.Idle }
    }

    private fun updateBridgeState() {
        val c = queueManager.getCurrentNotification()
        bridgeState = if (c != null) BridgeState.ShowingNotification(c, queueManager.size(), queueManager.getCurrentIndex())
        else BridgeState.WaitingForNotifications
    }

    private fun releaseDisplayController() {
        displayController?.let { try { it.close() } catch (_: Exception) {} }
        displayController = null
    }

    private fun releaseAllResources() {
        releaseDisplayController()
        demoRunnable?.let { handler.removeCallbacks(it) }
        tts?.stop(); tts?.shutdown(); tts = null
    }
}
