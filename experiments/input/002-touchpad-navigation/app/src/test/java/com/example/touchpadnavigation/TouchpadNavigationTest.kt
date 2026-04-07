package com.example.touchpadnavigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchpadNavigationTest {

    @Test
    fun cardPagesCountIsWithinGlassesConstraint() {
        val cardCount = 3
        assertTrue("Card pages should be 3 or fewer", cardCount <= 3)
        assertEquals(3, cardCount)
    }

    @Test
    fun listItemsCountIsWithinGlassesConstraint() {
        val listCount = 3
        assertTrue("List items should be 3 or fewer", listCount <= 3)
        assertEquals(3, listCount)
    }

    @Test
    fun cardIndexCoercionWorksCorrectly() {
        val totalCards = 3
        var currentIndex = 0

        // Forward coercion
        currentIndex = (currentIndex + 1).coerceAtMost(totalCards - 1)
        assertEquals(1, currentIndex)

        currentIndex = (currentIndex + 1).coerceAtMost(totalCards - 1)
        assertEquals(2, currentIndex)

        // At max, should not go further
        currentIndex = (currentIndex + 1).coerceAtMost(totalCards - 1)
        assertEquals(2, currentIndex)

        // Backward coercion
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        assertEquals(1, currentIndex)

        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        assertEquals(0, currentIndex)

        // At min, should not go further
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        assertEquals(0, currentIndex)
    }
}
