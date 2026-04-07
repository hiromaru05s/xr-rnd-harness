package com.example.notifvoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationQueueManagerTest {

    private fun makeItem(id: Int): NotificationItem {
        return NotificationItem(id = id, appName = "App", title = "Title $id", text = "Text $id")
    }

    @Test
    fun testAddNotification() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        assertEquals(1, mgr.size())
        assertNotNull(mgr.getCurrentNotification())
    }

    @Test
    fun testMaxSizeLimit() {
        val mgr = NotificationQueueManager(maxSize = 3)
        mgr.addNotification(makeItem(1))
        mgr.addNotification(makeItem(2))
        mgr.addNotification(makeItem(3))
        mgr.addNotification(makeItem(4))
        assertEquals(3, mgr.size())
    }

    @Test
    fun testMoveToNext() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        mgr.addNotification(makeItem(2))
        val next = mgr.moveToNext()
        assertNotNull(next)
        assertEquals(1, next!!.id)
    }

    @Test
    fun testMoveToPrevious() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        mgr.addNotification(makeItem(2))
        mgr.moveToNext()
        val prev = mgr.moveToPrevious()
        assertNotNull(prev)
        assertEquals(2, prev!!.id)
    }

    @Test
    fun testDismissCurrent() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        mgr.addNotification(makeItem(2))
        mgr.dismissCurrent()
        assertEquals(1, mgr.size())
    }

    @Test
    fun testDismissAll() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        mgr.dismissCurrent()
        assertTrue(mgr.isEmpty())
        assertNull(mgr.getCurrentNotification())
    }

    @Test
    fun testNewestFirst() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        mgr.addNotification(makeItem(2))
        assertEquals(2, mgr.getCurrentNotification()!!.id)
    }

    @Test
    fun testNavigationBounds() {
        val mgr = NotificationQueueManager()
        mgr.addNotification(makeItem(1))
        mgr.moveToPrevious()
        assertEquals(0, mgr.getCurrentIndex())
        mgr.moveToNext()
        assertEquals(0, mgr.getCurrentIndex())
    }
}
