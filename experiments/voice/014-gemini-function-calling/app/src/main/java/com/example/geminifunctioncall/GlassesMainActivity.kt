package com.example.geminifunctioncall

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.ProjectedDeviceController.Capability.Companion.CAPABILITY_VISUAL_UI
import androidx.xr.projected.ProjectedDisplayController
import androidx.xr.projected.ProjectedDisplayController.PresentationMode
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.example.geminifunctioncall.ui.FunctionCallScreen
import com.example.geminifunctioncall.ui.AgentState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(ExperimentalProjectedApi::class)
class GlassesMainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "GlassesFunctionCall"
    }

    private var displayController: ProjectedDisplayController? = null
    private var isVisualUiSupported by mutableStateOf(false)
    private var areVisualsOn by mutableStateOf(true)
    private var agentState by mutableStateOf(AgentState.Disconnected)
    val itemList = mutableStateListOf<String>()

    private val addItemDecl = FunctionDeclaration(
        name = "addItem",
        description = "Add an item to the shopping list",
        parameters = mapOf("item" to Schema.string("The item name to add"))
    )
    private val removeItemDecl = FunctionDeclaration(
        name = "removeItem",
        description = "Remove an item from the shopping list",
        parameters = mapOf("item" to Schema.string("The item name to remove"))
    )
    private val listItemsDecl = FunctionDeclaration(
        name = "listItems",
        description = "List all items currently in the shopping list",
        parameters = mapOf()
    )

    private fun handleFunctionCall(call: FunctionCallPart): FunctionResponsePart {
        return when (call.name) {
            "addItem" -> {
                val name = call.args?.get("item")?.toString()?.trim('"') ?: "unknown"
                itemList.add(name)
                FunctionResponsePart(call.name, JsonObject(mapOf(
                    "success" to JsonPrimitive(true),
                    "message" to JsonPrimitive("Added " + name + ". Total: " + itemList.size)
                )))
            }
            "removeItem" -> {
                val name = call.args?.get("item")?.toString()?.trim('"') ?: "unknown"
                val ok = itemList.remove(name)
                FunctionResponsePart(call.name, JsonObject(mapOf(
                    "success" to JsonPrimitive(ok),
                    "message" to JsonPrimitive(if (ok) "Removed " + name else name + " not found")
                )))
            }
            "listItems" -> {
                val items = if (itemList.isEmpty()) "empty" else itemList.joinToString(", ")
                FunctionResponsePart(call.name, JsonObject(mapOf(
                    "success" to JsonPrimitive(true),
                    "items" to JsonPrimitive(items),
                    "count" to JsonPrimitive(itemList.size)
                )))
            }
            else -> FunctionResponsePart(call.name, JsonObject(mapOf(
                "error" to JsonPrimitive("Unknown function")
            )))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { releaseDisplayController() }
        })
        initializeGlassesFeatures()
        setContent {
            GlimmerTheme {
                if (isVisualUiSupported && areVisualsOn) {
                    FunctionCallScreen(
                        agentState = agentState,
                        items = itemList.toList(),
                        onConnect = { connectGemini() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); releaseDisplayController(); initializeGlassesFeatures() }
    override fun onResume() { super.onResume(); if (displayController == null) initializeGlassesFeatures() }
    override fun onStop() { super.onStop(); if (isFinishing) releaseDisplayController() }

    private fun connectGemini() {
        agentState = AgentState.Connecting
        lifecycleScope.launch {
            try {
                val tools = listOf(Tool.functionDeclarations(listOf(addItemDecl, removeItemDecl, listItemsDecl)))
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                    modelName = "gemini-2.5-flash-native-audio-preview-12-2025",
                    generationConfig = liveGenerationConfig {
                        responseModality = ResponseModality.AUDIO
                        speechConfig = SpeechConfig(voice = Voice("FENRIR"))
                    },
                    systemInstruction = content {
                        text("You are a shopping list assistant for AI glasses. Use functions to manage lists. Confirm actions verbally.")
                    },
                    tools = tools,
                )
                val session = model.connect()
                agentState = AgentState.Connected
                session.startAudioConversation(functionCallHandler = ::handleFunctionCall)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini connect failed", e)
                agentState = AgentState.Error
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
            } catch (e: Exception) { Log.e(TAG, "Init failed", e) }
        }
    }

    private fun releaseDisplayController() {
        displayController?.let { ctrl -> try { ctrl.close() } catch (e: Exception) { Log.w(TAG, "Error", e) } }
        displayController = null
    }
}
