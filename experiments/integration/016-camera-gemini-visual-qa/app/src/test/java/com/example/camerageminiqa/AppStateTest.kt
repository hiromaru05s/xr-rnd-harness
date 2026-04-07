package com.example.camerageminiqa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {

    @Test
    fun testInitializingState() {
        val state: AppState = AppState.Initializing
        assertTrue(state is AppState.Initializing)
    }

    @Test
    fun testReadyState() {
        val state: AppState = AppState.Ready
        assertTrue(state is AppState.Ready)
    }

    @Test
    fun testCapturingState() {
        val state: AppState = AppState.Capturing
        assertTrue(state is AppState.Capturing)
    }

    @Test
    fun testAnalyzingDefaultDescription() {
        val state = AppState.Analyzing()
        assertTrue(state.imageDescription.isNotEmpty())
    }

    @Test
    fun testAnalyzingCustomDescription() {
        val state = AppState.Analyzing(imageDescription = "Custom")
        assertEquals("Custom", state.imageDescription)
    }

    @Test
    fun testConversingState() {
        val state = AppState.Conversing(lastTranscript = "Hello")
        assertEquals("Hello", state.lastTranscript)
    }

    @Test
    fun testConversingDefaultTranscript() {
        val state = AppState.Conversing()
        assertEquals("", state.lastTranscript)
    }

    @Test
    fun testErrorState() {
        val state = AppState.Error(message = "Test error")
        assertEquals("Test error", state.message)
    }

    @Test
    fun testStateTransition() {
        var currentState: AppState = AppState.Ready
        currentState = AppState.Capturing
        assertTrue(currentState is AppState.Capturing)
        currentState = AppState.Analyzing()
        assertTrue(currentState is AppState.Analyzing)
    }
}
