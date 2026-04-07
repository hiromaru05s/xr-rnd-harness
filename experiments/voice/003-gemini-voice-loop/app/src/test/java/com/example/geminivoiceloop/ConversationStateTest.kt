package com.example.geminivoiceloop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ConversationStateTest {

    @Test
    fun allStatesHaveDisplayLabels() {
        ConversationState.entries.forEach { state ->
            assertNotNull("${state.name} should have a display label", state.displayLabel)
        }
    }

    @Test
    fun stateTransitionsAreValid() {
        // IDLE -> CONNECTING -> LISTENING -> THINKING -> SPEAKING -> IDLE
        assertEquals("Ready", ConversationState.IDLE.displayLabel)
        assertEquals("Connecting...", ConversationState.CONNECTING.displayLabel)
        assertEquals("Listening...", ConversationState.LISTENING.displayLabel)
        assertEquals("Thinking...", ConversationState.THINKING.displayLabel)
        assertEquals("Speaking...", ConversationState.SPEAKING.displayLabel)
        assertEquals("Error", ConversationState.ERROR.displayLabel)
    }

    @Test
    fun stateCountIs6() {
        assertEquals(6, ConversationState.entries.size)
    }
}
