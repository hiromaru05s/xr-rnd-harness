package com.example.notifvoice

data class NotificationItem(
    val id: Int,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
)
