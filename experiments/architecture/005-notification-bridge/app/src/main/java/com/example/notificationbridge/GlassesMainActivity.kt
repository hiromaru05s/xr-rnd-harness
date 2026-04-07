package com.example.notificationbridge

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
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.notificationbridge.ui.NotificationBridgeScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesMainActivity"
    }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private lateinit var notificationHelper: NotificationHelper
    private var notificationCount by mutableIntStateOf(0)
    private var lastResult by mutableStateOf("待機中")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
            }
        })

        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    NotificationBridgeScreen(
                        notificationCount = notificationCount,
                        lastResult = lastResult,
                        onSendStandard = { sendStandardNotification() },
                        onSendMessaging = { sendMessagingNotification() },
                        onCancelAll = {
                            notificationHelper.cancelAllNotifications()
                            lastResult = "全通知キャンセル済み"
                        },
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
        if (isFinishing) releaseDisplayController()
    }

    private fun sendStandardNotification() {
        notificationCount++
        val result = notificationHelper.sendStandardNotification(
            notificationId = notificationCount,
            title = "グラス通知 #$notificationCount",
            content = "ProjectedExtender付き標準通知テスト",
        )
        lastResult = when (result) {
            is NotificationHelper.NotificationResult.Success -> "標準通知送信: #${result.notificationId}"
            is NotificationHelper.NotificationResult.Error -> "エラー: ${result.message}"
        }
    }

    private fun sendMessagingNotification() {
        notificationCount++
        val result = notificationHelper.sendMessagingNotification(
            notificationId = notificationCount + 100,
            senderName = "Gemini",
            message = "こんにちは。AIグラスの通知テストです。",
        )
        lastResult = when (result) {
            is NotificationHelper.NotificationResult.Success -> "会話通知送信: #${result.notificationId}"
            is NotificationHelper.NotificationResult.Error -> "エラー: ${result.message}"
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
        displayController?.let { controller ->
            try { controller.close() } catch (e: Exception) {
                Log.w(TAG, "Error closing DisplayController", e)
            }
        }
        displayController = null
    }
}
