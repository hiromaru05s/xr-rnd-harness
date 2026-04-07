package com.example.camerageminiqa

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig

/**
 * Gemini Live APIによるマルチモーダル（画像+音声）会話管理。
 * 撮影画像をセッションに送信し、音声で質疑応答を行う。
 */
class GeminiVisionManager {

    companion object {
        private const val TAG = "GeminiVisionManager"
        private const val MODEL_NAME = "gemini-2.5-flash-native-audio-preview-12-2025"
    }

    private var session: LiveSession? = null
    private var isSessionActive = false

    /**
     * Gemini Liveセッションを初期化して接続。
     * systemInstructionで画像分析アシスタントとしての役割を設定。
     */
    suspend fun initialize(): Boolean {
        return try {
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                modelName = MODEL_NAME,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice("FENRIR"))
                },
                systemInstruction = content {
                    text(
                        "You are a visual assistant for AI glasses. " +
                        "When the user sends an image, describe what you see briefly and clearly. " +
                        "Answer follow-up questions about the image concisely. " +
                        "Keep all responses under 3 sentences for glanceable display."
                    )
                },
            )
            session = model.connect()
            isSessionActive = true
            Log.d(TAG, "Gemini Live session connected")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemini Live session", e)
            false
        }
    }

    /**
     * 音声会話を開始。
     * transcriptHandlerでリアルタイムのトランスクリプトを受け取る。
     */
    suspend fun startConversation(
        onTranscript: (String) -> Unit,
    ) {
        val currentSession = session ?: return
        try {
            currentSession.startAudioConversation(
                transcriptHandler = { transcript ->
                    onTranscript(transcript)
                },
                enableInterruptions = true,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio conversation", e)
            throw e
        }
    }

    /**
     * キャプチャした画像をGeminiセッションに送信。
     * Base64エンコードされたJPEG画像をInlineDataとして送る。
     */
    suspend fun sendImage(base64Image: String, mimeType: String = "image/jpeg") {
        val currentSession = session ?: return
        try {
            val imageData = android.util.Base64.decode(base64Image, android.util.Base64.NO_WRAP)
            val content = content {
                inlineData(mimeType, imageData)
                text("What do you see in this image? Describe it briefly.")
            }
            currentSession.send(content)
            Log.d(TAG, "Image sent to Gemini session")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send image to Gemini", e)
            throw e
        }
    }

    /**
     * 音声会話を停止。
     */
    fun stopConversation() {
        try {
            session?.stopAudioConversation()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping conversation", e)
        }
    }

    /**
     * セッションを閉じてリソースを解放。
     */
    fun release() {
        try {
            session?.stopAudioConversation()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping conversation during release", e)
        }
        try {
            session?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing session", e)
        }
        session = null
        isSessionActive = false
    }
}
