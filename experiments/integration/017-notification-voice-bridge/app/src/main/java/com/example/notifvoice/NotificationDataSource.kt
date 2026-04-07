package com.example.notifvoice

/**
 * Simulates incoming notifications for demo purposes.
 * In production, this would be replaced by NotificationListenerService.
 */
object NotificationDataSource {

    private var nextId = 1

    private val sampleNotifications = listOf(
        Triple("Messages", "John Smith", "Hey, are you free for lunch today?"),
        Triple("Calendar", "Team Meeting", "Starts in 15 minutes - Room 301"),
        Triple("Email", "Project Update", "The deployment is complete"),
        Triple("Weather", "Rain Alert", "Rain expected in 30 minutes"),
        Triple("Maps", "Navigation", "Turn right in 200 meters"),
    )

    fun generateNext(): NotificationItem {
        val idx = (nextId - 1) % sampleNotifications.size
        val (app, title, text) = sampleNotifications[idx]
        return NotificationItem(
            id = nextId++,
            appName = app,
            title = title,
            text = text,
        )
    }
}
