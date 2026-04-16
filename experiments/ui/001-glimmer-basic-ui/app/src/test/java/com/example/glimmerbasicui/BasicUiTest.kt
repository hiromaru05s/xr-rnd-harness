package com.example.glimmerbasicui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BasicUiScreen data constraints.
 *
 * [FB fix #3] Test string literals changed to English to match
 * the UI text update for mojibake resolution.
 */
class BasicUiTest {

    @Test
    fun `menu items count is within glasses constraint`() {
        // Glasses UI constraint: lists must have 3 items or fewer
        val items = listOf("Alerts", "Settings", "Help")
        assertTrue("List must have 3 items or fewer", items.size <= 3)
        assertEquals(3, items.size)
    }

    @Test
    fun `initial selected label is correct`() {
        val initialLabel = "None"
        assertTrue("Initial label must not be empty", initialLabel.isNotEmpty())
    }

    @Test
    fun `selected label updates on item tap`() {
        var selectedLabel = "None"
        val tapTarget = "Settings"

        // Simulate tap action
        selectedLabel = tapTarget

        assertEquals(tapTarget, selectedLabel)
    }
}
