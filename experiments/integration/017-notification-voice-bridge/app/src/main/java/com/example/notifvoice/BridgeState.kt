package com.example.notifvoice

sealed class BridgeState {
    data object Initializing : BridgeState()
    data object WaitingForNotifications : BridgeState()
    data class ShowingNotification(
        val current: NotificationItem,
        val queueSize: Int,
        val currentIndex: Int,
    ) : BridgeState()
    data class Reading(
        val current: NotificationItem,
        val queueSize: Int,
        val currentIndex: Int,
    ) : BridgeState()
    data class Error(val message: String) : BridgeState()
}

enum class TtsStatus { Idle, Speaking, Completed, ErrorOccurred }
