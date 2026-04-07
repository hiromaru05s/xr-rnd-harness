package com.example.ttsaudiofeedback

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log

/**
 * TTS音声フィードバック管理クラス。
 *
 * Android内蔵TTSを使って音声フィードバックを管理する。
 * UtteranceProgressListenerで発話完了を検知し、
 * コールバックでUI更新を通知する。
 */
class TtsManager(
    context: Context,
    private val onStateChanged: (TtsState) -> Unit,
) {
    companion object {
        private const val TAG = "TtsManager"
    }

    /** TTS状態のsealed class */
    sealed class TtsState {
        data object Initializing : TtsState()
        data object Ready : TtsState()
        data class Speaking(val utteranceId: String) : TtsState()
        data class Completed(val utteranceId: String) : TtsState()
        data class Error(val message: String) : TtsState()
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var utteranceCounter = 0

    init {
        onStateChanged(TtsState.Initializing)
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupProgressListener()
                onStateChanged(TtsState.Ready)
                Log.d(TAG, "TTS initialized successfully")
            } else {
                onStateChanged(TtsState.Error("TTS initialization failed: status=$status"))
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                onStateChanged(TtsState.Speaking(utteranceId))
                Log.d(TAG, "TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String) {
                onStateChanged(TtsState.Completed(utteranceId))
                Log.d(TAG, "TTS completed: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                onStateChanged(TtsState.Error("Utterance error: $utteranceId"))
                Log.e(TAG, "TTS error: $utteranceId")
            }
        })
    }

    /**
     * テキストを即座に読み上げる（他の発話を中断）。
     * @return utteranceId（発話完了の追跡に使用）
     */
    fun speakFlush(text: String): String? {
        if (!isInitialized) {
            onStateChanged(TtsState.Error("TTS not initialized"))
            return null
        }
        val id = "utterance_${++utteranceCounter}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        return id
    }

    /**
     * テキストをキューに追加して読み上げる（他の発話の後に続く）。
     * @return utteranceId
     */
    fun speakAdd(text: String): String? {
        if (!isInitialized) {
            onStateChanged(TtsState.Error("TTS not initialized"))
            return null
        }
        val id = "utterance_${++utteranceCounter}"
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, id)
        return id
    }

    /** 現在の発話を停止 */
    fun stop() {
        tts?.stop()
        if (isInitialized) onStateChanged(TtsState.Ready)
    }

    /** リソースを解放。onDestroyで必ず呼ぶ */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.d(TAG, "TTS shutdown")
    }
}
