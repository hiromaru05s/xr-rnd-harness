package com.example.geminivoiceloop

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
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.geminivoiceloop.ui.VoiceLoopScreen
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)

    var conversationState by mutableStateOf(ConversationState.IDLE)
        private set
    var lastTranscript by mutableStateOf("")
        private set
    var lastResponse by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseDisplayController()
                tts?.shutdown()
                tts = null
            }
        })

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS initialized")
            }
        }

        initializeGlassesFeatures()

        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    VoiceLoopScreen(
                        conversationState = conversationState,
                        lastTranscript = lastTranscript,
                        lastResponse = lastResponse,
                        errorMessage = errorMessage,
                        onStartConversation = { startConversation() },
                        onStopConversation = { stopConversation() },
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
        if (isFinishing) {
            stopConversation()
            releaseDisplayController()
        }
    }

    private fun startConversation() {
        if (conversationState == ConversationState.LISTENING) return
        conversationState = ConversationState.CONNECTING
        errorMessage = null

        lifecycleScope.launch {
            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                    modelName = "gemini-2.5-flash-native-audio-preview-12-2025",
                    generationConfig = liveGenerationConfig {
                        responseModality = ResponseModality.AUDIO
                        speechConfig = SpeechConfig(voice = Voice("FENRIR"))
                    },
                    systemInstruction = content {
                        text("You are a helpful assistant for AI glasses users. Keep responses brief.")
                    },
                )
                val session = model.connect()
                conversationState = ConversationState.LISTENING

                session.startAudioConversation(
                    transcriptHandler = { transcript -> lastTranscript = transcript },
                    enableInterruptions = true,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start conversation", e)
                conversationState = ConversationState.ERROR
                errorMessage = "Connection failed: \${e.message}"
                tts?.speak("Voice connection failed.", TextToSpeech.QUEUE_FLUSH, null, "err")
            }
        }
    }

    private fun stopConversation() {
        conversationState = ConversationState.IDLE
        lastTranscript = ""
        lastResponse = ""
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
        displayController?.let { c -> try { c.close() } catch (_: Exception) {} }
        displayController = null
    }

    companion object { private const val TAG = "GlassesMainActivity" }
}

enum class ConversationState(val displayLabel: String) {
    IDLE("Ready"),
    CONNECTING("Connecting..."),
    LISTENING("Listening..."),
    THINKING("Thinking..."),
    SPEAKING("Speaking..."),
    ERROR("Error"),
}
