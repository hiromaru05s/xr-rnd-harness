package com.example.notifvoice

/**
 * Manages a rolling queue of notifications (max 3 for FOV compliance).
 * Provides navigation (next/previous) and dismiss operations.
 */
class NotificationQueueManager(private val maxSize: Int = 3) {

    private val queue = mutableListOf<NotificationItem>()
    private var currentIndex = 0

    fun addNotification(item: NotificationItem) {
        queue.add(0, item)
        if (queue.size > maxSize) { queue.removeAt(queue.lastIndex) }
        currentIndex = 0
    }

    fun getCurrentNotification(): NotificationItem? {
        return queue.getOrNull(currentIndex)
    }

    fun moveToNext(): NotificationItem? {
        if (currentIndex < queue.size - 1) { currentIndex++ }
        return getCurrentNotification()
    }

    fun moveToPrevious(): NotificationItem? {
        if (currentIndex > 0) { currentIndex-- }
        return getCurrentNotification()
    }

    fun dismissCurrent(): NotificationItem? {
        if (queue.isEmpty()) return null
        queue.removeAt(currentIndex)
        if (currentIndex >= queue.size && currentIndex > 0) { currentIndex-- }
        return getCurrentNotification()
    }

    fun getAll(): List<NotificationItem> = queue.toList()

    fun size(): Int = queue.size

    fun getCurrentIndex(): Int = currentIndex

    fun isEmpty(): Boolean = queue.isEmpty()
}
