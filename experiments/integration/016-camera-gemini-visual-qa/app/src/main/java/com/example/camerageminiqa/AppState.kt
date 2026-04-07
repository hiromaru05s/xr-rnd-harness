package com.example.camerageminiqa

/**
 * 統合ステートマシン: カメラキャプチャ + Gemini会話の全状態を管理
 *
 * 状態遷移:
 * Initializing → Ready → Capturing → Analyzing → Conversing → Ready
 *                  ↑                                           │
 *                  └───────────────────────────────────────────┘
 * どの状態からもError遷移可能。ErrorからRetryでReadyに戻る。
 */
sealed class AppState {
    /** 初期化中（カメラ/Geminiセッション準備） */
    data object Initializing : AppState()

    /** 準備完了。カメラボタンで撮影可能 */
    data object Ready : AppState()

    /** 画像キャプチャ中 */
    data object Capturing : AppState()

    /** Geminiに画像送信・分析中 */
    data class Analyzing(val imageDescription: String = "画像を分析中...") : AppState()

    /** Geminiと音声会話中 */
    data class Conversing(val lastTranscript: String = "") : AppState()

    /** エラー発生 */
    data class Error(val message: String) : AppState()
}
